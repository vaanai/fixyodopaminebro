# Release builds are shrunk with R8. Nothing here uses reflection: the services are kept through the
# manifest and the rule files are plain Java resources, which R8 keeps.

# Readable stack traces in Play Console crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
