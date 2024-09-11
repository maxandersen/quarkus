package io.quarkus.annotation.processor.documentation.config.scanner;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryRootElement;
import io.quarkus.annotation.processor.documentation.config.discovery.ParsedJavadoc;
import io.quarkus.annotation.processor.documentation.config.discovery.ParsedJavadocSection;
import io.quarkus.annotation.processor.documentation.config.discovery.ResolvedType;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements.JavadocElement;
import io.quarkus.annotation.processor.documentation.config.util.JavadocUtil;
import io.quarkus.annotation.processor.documentation.config.util.Markers;
import io.quarkus.annotation.processor.documentation.config.util.Types;
import io.quarkus.annotation.processor.util.Config;
import io.quarkus.annotation.processor.util.Utils;

/**
 * This class is responsible for collecting and writing the Javadoc.
 */
public class JavadocLegacyConfigRootListener extends AbstractJavadocConfigListener {

    JavadocLegacyConfigRootListener(Config config, Utils utils, ConfigCollector configCollector) {
        super(config, utils, configCollector);
    }

    @Override
    public void onEnclosedField(DiscoveryRootElement discoveryRootElement, TypeElement clazz, VariableElement field,
            ResolvedType resolvedType) {
        if (config.getExtension().isMixedModule() && discoveryRootElement.isConfigMapping()) {
            return;
        }

        // we only get Javbadoc for local classes
        // classes coming from other modules won't have Javadoc available
        if (!utils.element().isLocalClass(clazz)) {
            return;
        }

        String rawJavadoc = utils.element().getJavadoc(field).orElse("");
        boolean isSection = utils.element().isAnnotationPresent(field, Types.ANNOTATION_CONFIG_DOC_SECTION);

        if (isSection) {
            // for sections, we only keep the title
            ParsedJavadocSection parsedJavadocSection = JavadocUtil.parseConfigSectionJavadoc(rawJavadoc);

            if (parsedJavadocSection.title() == null) {
                return;
            }

            configCollector.addJavadocElement(
                    clazz.getQualifiedName().toString() + Markers.DOT + field.getSimpleName().toString(),
                    new JavadocElement(parsedJavadocSection.title(), parsedJavadocSection.format(), null,
                            parsedJavadocSection.deprecated()));
        } else {
            ParsedJavadoc parsedJavadoc = JavadocUtil.parseConfigItemJavadoc(rawJavadoc);

            // We require a Javadoc for config items that are not config groups except if they are a section
            if (parsedJavadoc.description() == null) {
                if (parsedJavadoc.deprecated() == null && !resolvedType.isConfigGroup()) {
                    utils.element().addMissingJavadocError(field);
                }
                return;
            }

            configCollector.addJavadocElement(
                    clazz.getQualifiedName().toString() + Markers.DOT + field.getSimpleName().toString(),
                    new JavadocElement(parsedJavadoc.description(), parsedJavadoc.format(), parsedJavadoc.since(),
                            parsedJavadoc.deprecated()));
        }
    }
}
