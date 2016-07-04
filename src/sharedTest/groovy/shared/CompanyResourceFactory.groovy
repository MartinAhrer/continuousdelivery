package shared

import groovy.json.JsonBuilder

class CompanyResourceFactory {
    Closure contentWriter = { def content -> content }
    def newResource() {
        JsonBuilder builder = new JsonBuilder()
        builder {
            name 'ACME'
            address (
                line1: 'Post street 1',
                line2: 'Post Street 2'
            )
        }
        contentWriter (builder.content)
    }
}
