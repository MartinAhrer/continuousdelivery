package at.martinahrer.cd;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.dialect.PostgreSQLPGObjectJdbcType;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.data.jpa.domain.AbstractPersistable;

import static org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS;

@SpringBootApplication
@Slf4j
@ImportRuntimeHints(Application.ApplicationHints.class)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    static class ApplicationHints implements RuntimeHintsRegistrar {
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.reflection().registerType(AbstractPersistable.class, DECLARED_FIELDS, INVOKE_DECLARED_METHODS);

            hints.reflection().registerTypeIfPresent(classLoader, "org.postgresql.util.PGobject",
                    (hint) -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INTROSPECT_PUBLIC_METHODS)
                            .onReachableType(PostgreSQLPGObjectJdbcType.class));
        }
    }
}
