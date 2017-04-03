import hudson.model.*
import jenkins.model.*

import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*

def dockerRegistryUser = System.getenv('DOCKER_REGISTRY_USER')
def dockerRegistryPassword = System.getenv('DOCKER_REGISTRY_PASSWORD')

if (dockerRegistryUser && dockerRegistryPassword) {

    def config = new HashMap()
    config.putAll(getBinding().getVariables())
    def out = config['out']

    Credentials credentials = (Credentials) new UsernamePasswordCredentialsImpl(
            CredentialsScope.GLOBAL,
            "dockerRegistryDeployer",
            "Docker registry credentials",
            dockerRegistryUser,
            dockerRegistryPassword)

    def instance = Jenkins.getInstance()

    def credentialsStore = instance.getExtensionList(SystemCredentialsProvider.class)[0].store
    credentialsStore.addCredentials(Domain.global(), credentials)

    instance.save()
    out.println "Added credentials 'dockerRegistryDeployer' with credentials from DOCKER_REGISTRY_USER:DOCKER_REGISTRY_PASSWORD"
}
