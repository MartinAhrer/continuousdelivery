import hudson.model.*
import jenkins.model.*

import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*


def scmUser = System.getenv('SCM_USER')
def scmPassword = System.getenv('SCM_PASSWORD')

if (scmUser && scmPassword) {

    def config = new HashMap()
    config.putAll(getBinding().getVariables())
    def out = config['out']

    Credentials credentials = (Credentials) new UsernamePasswordCredentialsImpl(
            CredentialsScope.GLOBAL,
            "scm",
            "Source code repository credentials",
            scmUser,
            scmPassword)

    def instance = Jenkins.getInstance()

    def credentialsStore = instance.getExtensionList(SystemCredentialsProvider.class)[0].store
    credentialsStore.addCredentials(Domain.global(), credentials)

    instance.save()
    out.println "Added credentials 'scm' with credentials from SCM_USER:SCM_PASSWORD"
}
