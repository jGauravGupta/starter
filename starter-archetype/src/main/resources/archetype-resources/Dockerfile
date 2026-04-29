#if (!${payaraVersion})
#if (${jakartaEEVersion} == '8')
#set ($payaraVersion = "5.2022.5")
#else
#set ($payaraVersion = "6.2024.2")
#end
#end
## Determine the effective JDK tag for the Docker base image.
## JDK 11 images are not published for payaraVersion >= 6.2024.9; fall back to JDK 17.
#set ($dockerJavaVersion = ${javaVersion})
#if (${javaVersion} == '11')
#set ($dot1 = $payaraVersion.indexOf('.'))
#set ($dot2 = $payaraVersion.lastIndexOf('.'))
#set ($pvYear = $payaraVersion.substring($dot1 + 1, $dot2))
#set ($pvMonth = $payaraVersion.substring($dot2 + 1))
#if ($pvYear > "2024" || ($pvYear == "2024" && ($pvMonth == "9" || $pvMonth == "10" || $pvMonth == "11" || $pvMonth == "12")))
#set ($dockerJavaVersion = "17")
#end
#end
#if (${platform} == 'server')
#if (${dockerJavaVersion} == '21')
#if (${profile} == 'full')
#set ($baseImage = "payara/server-full:${payaraVersion}-jdk21")
#else
#set ($baseImage = "payara/server-web:${payaraVersion}-jdk21")
#end
#elseif (${dockerJavaVersion} == '17')
#if (${profile} == 'full')
#set ($baseImage = "payara/server-full:${payaraVersion}-jdk17")
#else
#set ($baseImage = "payara/server-web:${payaraVersion}-jdk17")
#end
#elseif (${dockerJavaVersion} == '11')
#if (${profile} == 'full')
#set ($baseImage = "payara/server-full:${payaraVersion}-jdk11")
#else
#set ($baseImage = "payara/server-web:${payaraVersion}-jdk11")
#end
#else
#if (${profile} == 'full')
#set ($baseImage = "payara/server-full:${payaraVersion}")
#else
#set ($baseImage = "payara/server-web:${payaraVersion}")
#end
#end
#elseif (${platform} == 'micro')
#if (${dockerJavaVersion} == '21')
#set ($baseImage = "payara/micro:${payaraVersion}-jdk21")
#elseif (${dockerJavaVersion} == '17')
#set ($baseImage = "payara/micro:${payaraVersion}-jdk17")
#elseif (${dockerJavaVersion} == '11')
#set ($baseImage = "payara/micro:${payaraVersion}-jdk11")
#else
#set ($baseImage = "payara/micro:${payaraVersion}")
#end
#end
#if (${build} == 'maven')
#set ($artifactParentDir = "target")
#else
#set ($artifactParentDir = "build/libs")
#end
FROM $baseImage
COPY $artifactParentDir/${artifactId}-${version}.war $DEPLOY_DIR
