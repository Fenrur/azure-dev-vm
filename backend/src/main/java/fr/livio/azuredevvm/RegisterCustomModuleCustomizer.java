package fr.livio.azuredevvm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigpwned.jackson.modules.jdk17.sealedclasses.Jdk17SealedClassesModule;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

@Singleton
public class RegisterCustomModuleCustomizer implements ObjectMapperCustomizer {

    public void customize(ObjectMapper mapper) {
        mapper.registerModule(new Jdk17SealedClassesModule());
    }
}
