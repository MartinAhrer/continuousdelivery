import hudson.model.*
import jenkins.model.*

def instance = Jenkins.getInstance()

def globalNodeProperties = instance.getGlobalNodeProperties()
def environmentVariablesNodeProperty = globalNodeProperties.getAll(hudson.slaves.EnvironmentVariablesNodeProperty.class)

if (environmentVariablesNodeProperty == null || environmentVariablesNodeProperty.size() == 0) {
    globalNodeProperties.add(new hudson.slaves.EnvironmentVariablesNodeProperty())
}
environmentVariables = globalNodeProperties.getAll(hudson.slaves.EnvironmentVariablesNodeProperty.class).get(0).envVars

def variableNames = System.getenv('JENKINS_GLOBAL_PROPERTIES_IMPORT')
if (variableNames) {

    def config = new HashMap()
    config.putAll(getBinding().getVariables())
    def out = config['out']

    for (name in variableNames.split(/(;|,)/)) {
        out.println "Importing ${name} into environment"
        environmentVariables.put(name, System.getenv(name))
    }
}
instance.save()
