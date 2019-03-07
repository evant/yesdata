# yesdata
Errorprone check to verify you have implemented data classes correctly.

## Usage

Annotate your data classes with `@Data`. The checker will 
verify you implemented equals, hashCode, and toString. It will also check that you haven't forgotten 
to use a field in those implementations.

## Download

Gradle, using [`net.ltgt.errorprone` plugin](https://github.com/tbroyer/gradle-errorprone-plugin):

```groovy
dependencies {
  compileOnly 'com.willowtreeapps.yesdata:yesdata-annotations:1.0.0'
  errorprone 'com.willowtreeapps.yesdata:yesdata-checker:1.0.0'
}
```

By default the check will operate everywhere that error-prone runs.