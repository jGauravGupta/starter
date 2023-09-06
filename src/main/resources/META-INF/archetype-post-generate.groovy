
import org.apache.commons.io.FileUtils

def profile = request.properties["profile"].trim()
def jakartaEEVersion = request.properties["jakartaEEVersion"].trim()
def javaVersion = request.properties["javaVersion"].trim()
def platform = request.properties["platform"].trim()

def outputDirectory = new File(request.getOutputDirectory(), request.getArtifactId())

validateInput(profile, jakartaEEVersion, javaVersion, platform, outputDirectory)
generateSource(platform, jakartaEEVersion, outputDirectory)
bindEEPackage(jakartaEEVersion, outputDirectory)

private validateInput(profile, jakartaEEVersion, javaVersion, platform, File outputDirectory) {
    if ((jakartaEEVersion != '8') && (jakartaEEVersion != '9')
            && (jakartaEEVersion != '9.1') && (jakartaEEVersion != '10')) {
        FileUtils.forceDelete(outputDirectory)
        throw new RuntimeException("Failed, valid Jakarta EE versions are 8, 9, 9.1, and 10")
    }

    if (!profile.equalsIgnoreCase("core") && !profile.equalsIgnoreCase("web") && !profile.equalsIgnoreCase("full")) {
        FileUtils.forceDelete(outputDirectory)
        throw new RuntimeException("Failed, valid Jakarta EE profiles are core, web, and full")
    }

    if ((javaVersion != '8') && (javaVersion != '11') && (javaVersion != '17')) {
        FileUtils.forceDelete(outputDirectory)
        throw new RuntimeException("Failed, valid Java SE versions are 8, 11, and 17")
    }

    if (!platform.equalsIgnoreCase("server")
           && !platform.equalsIgnoreCase("micro")) {
        FileUtils.forceDelete(outputDirectory)
        throw new RuntimeException("Failed, valid platform values are server, and micro")
    }

    if (profile.equalsIgnoreCase("core") && jakartaEEVersion != '10') {
        FileUtils.forceDelete(outputDirectory)
        throw new RuntimeException("Failed, the Core Profile is only supported for Jakarta EE 10")
    }

    if ((jakartaEEVersion != '8') && (javaVersion == '8')) {
        FileUtils.forceDelete(outputDirectory)
        throw new RuntimeException("Failed, Payara 6 does not support Java SE 8")
    }

}

private generateSource(platform, jakartaEEVersion, File outputDirectory) {
    if (platform.equals("micro")) {
        FileUtils.forceDelete(new File(outputDirectory, "src/test/resources"))
    }
}

private bindEEPackage(jakartaEEVersion, File outputDirectory) {
    def eePackage = 'jakarta';
    if (jakartaEEVersion == '8') {
        eePackage = 'javax'
    }

    println "Binding EE package: " + eePackage

    def binding = ["eePackage": eePackage]
    def engine = new groovy.text.SimpleTemplateEngine()

    outputDirectory.traverse(type: groovy.io.FileType.FILES, nameFilter: ~/.*\.(xml|java)$/) { it ->
        if (!it.name.endsWith("pom.xml") && !it.name.endsWith("arquillian.xml")) {
            it.withReader('UTF-8') { reader ->
                try {
                    def template = engine.createTemplate(reader).make(binding)
                    new FileWriter(it).write(template)
                } catch (ignored) {
                    println ignored
                }
            }
        }
    }
}