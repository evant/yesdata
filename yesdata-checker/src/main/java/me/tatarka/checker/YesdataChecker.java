package me.tatarka.checker;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeKind;

import me.tatarka.yesdata.annotation.Data;

import static com.sun.source.tree.Tree.Kind.CLASS;

@AutoService(BugChecker.class)
@BugPattern(name = "Yesdata",
        summary = "Classes annotated with @Data should be implemented correctly.",
        explanation = "Data classes should be final, and implement equals, hashCode, and toString(). Those methods should use all the the class's fields.",
        linkType = BugPattern.LinkType.NONE,
        severity = BugPattern.SeverityLevel.ERROR)
public class YesdataChecker extends BugChecker implements BugChecker.ClassTreeMatcher, BugChecker.MethodTreeMatcher {
    private static final String DATA_CLASS_NAME = Data.class.getCanonicalName();

    private FoundMethods foundMethods;

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
        foundMethods = null;
        if (tree.getKind() != CLASS) {
            return Description.NO_MATCH;
        }
        ModifiersTree modifiers = tree.getModifiers();
        for (AnnotationTree annotation : modifiers.getAnnotations()) {
            AnnotationMirror annotationMirror = ASTHelpers.getAnnotationMirror(annotation);
            if (annotationMirror.getAnnotationType().toString().equals(DATA_CLASS_NAME)) {
                return checkDataClass(tree, state);
            }
        }
        return null;
    }

    private Description checkDataClass(ClassTree tree, VisitorState state) {
        DataClassChecker checker = new DataClassChecker(tree);
        foundMethods = checker.check();

        if (foundMethods.allFound()) {
            return Description.NO_MATCH;
        } else {
            StringBuilder message = new StringBuilder("Class does not implement ");
            List<String> missingMethods = new ArrayList<>();
            if (!foundMethods.contains("equals")) {
                missingMethods.add("equals");
            }
            if (!foundMethods.contains("hashCode")) {
                missingMethods.add("hashCode");
            }
            if (!foundMethods.contains("toString")) {
                missingMethods.add("toString");
            }
            formatList(message, missingMethods, "'");
            message.append(".");
            return buildDescription(tree)
                    .setMessage(message.toString())
                    .build();
        }
    }

    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
        return foundMethods == null ? Description.NO_MATCH : foundMethods.descriptionFor(tree);
    }

    class DataClassChecker {
        private final List<VariableTree> fields = new ArrayList<>();
        private final List<MethodTree> methods = new ArrayList<>();

        DataClassChecker(ClassTree tree) {
            for (Tree member : tree.getMembers()) {
                if (member instanceof MethodTree) {
                    MethodTree method = (MethodTree) member;
                    methods.add(method);
                } else if (member instanceof VariableTree) {
                    VariableTree variable = (VariableTree) member;
                    if (shouldIncludeVariable(variable)) {
                        fields.add(variable);
                    }
                }
            }
        }

        FoundMethods check() {
            FoundMethods foundMethods = new FoundMethods();
            for (MethodTree method : methods) {
                if (isEquals(method)) {
                    foundMethods.methods.put(method, checkImplementation(method));
                } else if (isHashCode(method)) {
                    foundMethods.methods.put(method, checkImplementation(method));
                } else if (isToString(method)) {
                    foundMethods.methods.put(method, checkImplementation(method));
                }
            }
            return foundMethods;
        }

        private boolean shouldIncludeVariable(VariableTree variable) {
            return !variable.getModifiers().getFlags().contains(Modifier.STATIC)
                    && !variable.getModifiers().getFlags().contains(Modifier.TRANSIENT);
        }

        private boolean isEquals(MethodTree method) {
            return method.getName().contentEquals("equals")
                    && method.getModifiers().getFlags().contains(Modifier.PUBLIC)
                    && method.getReturnType() instanceof PrimitiveTypeTree
                    && ((PrimitiveTypeTree) method.getReturnType()).getPrimitiveTypeKind() == TypeKind.BOOLEAN
                    && method.getParameters().size() == 1
                    && method.getParameters().get(0).getType() instanceof IdentifierTree
                    && ((IdentifierTree) method.getParameters().get(0).getType()).getName().contentEquals("Object");
        }

        private boolean isHashCode(MethodTree method) {
            return method.getName().contentEquals("hashCode")
                    && method.getModifiers().getFlags().contains(Modifier.PUBLIC)
                    && method.getReturnType() instanceof PrimitiveTypeTree
                    && ((PrimitiveTypeTree) method.getReturnType()).getPrimitiveTypeKind() == TypeKind.INT
                    && method.getParameters().size() == 0;
        }

        private boolean isToString(MethodTree method) {
            return method.getName().contentEquals("toString")
                    && method.getModifiers().getFlags().contains(Modifier.PUBLIC)
                    && method.getReturnType() instanceof IdentifierTree
                    && ((IdentifierTree) method.getReturnType()).getName().contentEquals("String")
                    && method.getParameters().size() == 0;
        }

        private Description checkImplementation(MethodTree method) {
            if (fields.isEmpty()) {
                return Description.NO_MATCH;
            }
            List<Name> foundNames = new ArrayList<>();
            method.accept(new TreeScanner<Void, List<Name>>() {
                @Override
                public Void visitIdentifier(IdentifierTree node, List<Name> foundNames) {
                    if (isField(node)) {
                        foundNames.add(node.getName());
                    }
                    return null;
                }

            }, foundNames);

            if (foundNames.size() == fields.size()) {
                return Description.NO_MATCH;
            }

            List<String> missingFields = new ArrayList<>(fields.size() - foundNames.size());
            for (VariableTree field : fields) {
                if (!foundNames.contains(field.getName())) {
                    missingFields.add(field.getName().toString());
                }
            }

            StringBuilder message = new StringBuilder("Missing ");
            message.append(missingFields.size() == 1 ? "field " : "fields ");
            formatList(message, missingFields, "'");
            message.append(" in ");
            message.append(method.getName());
            message.append(" implementation.");
            return buildDescription(method)
                    .setMessage(message.toString())
                    .build();
        }

        private boolean isField(IdentifierTree node) {
            for (VariableTree field : fields) {
                if (field.getName().equals(node.getName())) {
                    return true;
                }
            }
            return false;
        }
    }

    static class FoundMethods {
        Map<MethodTree, Description> methods = new LinkedHashMap<>();

        boolean allFound() {
            return methods.size() == 3;
        }

        boolean contains(String name) {
            for (MethodTree methodTree : methods.keySet()) {
                if (methodTree.getName().contentEquals(name)) {
                    return true;
                }
            }
            return false;
        }

        Description descriptionFor(MethodTree methodTree) {
            return methods.getOrDefault(methodTree, Description.NO_MATCH);
        }
    }

    private static void formatList(StringBuilder builder, List<?> items, String surround) {
        int size = items.size();
        if (size == 0) {
            return;
        }
        // a
        if (size == 1) {
            surround(builder, items.get(0), surround);
            return;
        }
        // a and b
        if (size == 2) {
            surround(builder, items.get(0), surround);
            builder.append(" and ");
            surround(builder, items.get(1), surround);
            return;
        }
        // a, b, ... and c
        for (int i = 0; i < size; i++) {
            Object item = items.get(i);
            if (i == size - 1) {
                builder.append("and ");
                surround(builder, item, surround);
            } else {
                surround(builder, item, surround);
                builder.append(", ");
            }
        }
    }

    private static void surround(StringBuilder builder, Object item, String surround) {
        builder.append(surround);
        builder.append(item);
        builder.append(surround);
    }
}
