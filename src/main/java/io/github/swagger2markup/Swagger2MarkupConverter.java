/*
 * Copyright 2016 Robert Winkler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.swagger2markup;

import com.google.common.annotations.VisibleForTesting;
import io.github.swagger2markup.builder.Swagger2MarkupConfigBuilder;
import io.github.swagger2markup.builder.Swagger2MarkupExtensionRegistryBuilder;
import io.github.swagger2markup.internal.document.builder.DefinitionsDocumentBuilder;
import io.github.swagger2markup.internal.document.builder.OverviewDocumentBuilder;
import io.github.swagger2markup.internal.document.builder.PathsDocumentBuilder;
import io.github.swagger2markup.internal.document.builder.SecurityDocumentBuilder;
import io.github.swagger2markup.spi.*;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;


/**
 * @author Robert Winkler
 */
public class Swagger2MarkupConverter {

    private Context context;

    public Swagger2MarkupConverter(Context globalContext) {
        this.context = globalContext;
    }

    /**
     * Returns the global Context
     *
     * @return the global Context
     */
    @VisibleForTesting
    Context getContext(){
        return context;
    }

    /**
     * Creates a Swagger2MarkupConverter.Builder using a remote URL.
     *
     * @param swaggerURL the remote URL
     * @return a Swagger2MarkupConverter
     */
    public static Builder from(URL swaggerURL){
        Validate.notNull(swaggerURL, "swaggerURL must not be null");
        return new Builder(swaggerURL);
    }

    /**
     * Creates a Swagger2MarkupConverter.Builder using a local Path.
     *
     * @param swaggerPath the local Path
     * @return a Swagger2MarkupConverter
     */
    public static Builder from(Path swaggerPath) {
        Validate.notNull(swaggerPath, "swaggerPath must not be null");
        return new Builder(swaggerPath);
    }

    /**
     * Creates a Swagger2MarkupConverter.Builder from a given Swagger model.
     *
     * @param swagger the Swagger source.
     * @return a Swagger2MarkupConverter
     */
    public static Builder from(Swagger swagger) {
        Validate.notNull(swagger, "swagger must not be null");
        return new Builder(swagger);
    }

    /**
     * Creates a Swagger2MarkupConverter.Builder from a given Swagger YAML or JSON String.
     *
     * @param swaggerString the Swagger YAML or JSON String.
     * @return a Swagger2MarkupConverter
     * @throws java.io.IOException if String can not be parsed
     */
    public static Builder from(String swaggerString) throws IOException {
        Validate.notEmpty(swaggerString, "swaggerString must not be null");
        return from(new StringReader(swaggerString));
    }

    /**
     * Creates a Swagger2MarkupConverter.Builder from a given Swagger YAML or JSON reader.
     *
     * @param swaggerReader the Swagger YAML or JSON reader.
     * @return a Swagger2MarkupConverter
     * @throws java.io.IOException if source can not be parsed
     */
    public static Builder from(Reader swaggerReader) throws IOException {
        Validate.notNull(swaggerReader, "swaggerReader must not be null");
        Swagger swagger = new SwaggerParser().parse(IOUtils.toString(swaggerReader));
        if (swagger == null)
            throw new IllegalArgumentException("Swagger source is in a wrong format");

        return new Builder(swagger);
    }

    /**
     * Builds the documents and stores the files in the given {@code outputDirectory}.
     *
     * @param outputDirectory the output directory path
     * @throws IOException if the files cannot be written
     */
    public void intoFolder(Path outputDirectory) throws IOException {
        Validate.notNull(outputDirectory, "outputDirectory must not be null");

        applySwaggerExtensions();
        
        new OverviewDocumentBuilder(context, outputDirectory).build().writeToFile(outputDirectory.resolve(context.config.getOverviewDocument()), StandardCharsets.UTF_8);
        new PathsDocumentBuilder(context, outputDirectory).build().writeToFile(outputDirectory.resolve(context.config.getPathsDocument()), StandardCharsets.UTF_8);
        new DefinitionsDocumentBuilder(context, outputDirectory).build().writeToFile(outputDirectory.resolve(context.config.getDefinitionsDocument()), StandardCharsets.UTF_8);
        new SecurityDocumentBuilder(context, outputDirectory).build().writeToFile(outputDirectory.resolve(context.config.getSecurityDocument()), StandardCharsets.UTF_8);
    }

    /**
     * Builds the document and stores it in the given {@code outputFile}.<br>
     * An extension identifying the markup language will be automatically added to file name.
     *
     * @param outputFile the output file
     * @throws IOException if the files cannot be written
     */
    public void toFile(Path outputFile) throws IOException {
        Validate.notNull(outputFile, "outputFile must not be null");

        new OverviewDocumentBuilder(context, null).build().writeToFile(outputFile, StandardCharsets.UTF_8);
        new PathsDocumentBuilder(context, null).build().writeToFile(outputFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        new DefinitionsDocumentBuilder(context, null).build().writeToFile(outputFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        new SecurityDocumentBuilder(context, null).build().writeToFile(outputFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    /**
     * Builds the document and stores it in the given {@code outputFile}.
     *
     * @param outputFile the output file
     * @throws IOException if the files cannot be written
     */
    public void toFileWithoutExtension(Path outputFile) throws IOException {
        Validate.notNull(outputFile, "outputFile must not be null");

        new OverviewDocumentBuilder(context, null).build().writeToFileWithoutExtension(outputFile, StandardCharsets.UTF_8);
        new PathsDocumentBuilder(context, null).build().writeToFileWithoutExtension(outputFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        new DefinitionsDocumentBuilder(context, null).build().writeToFileWithoutExtension(outputFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        new SecurityDocumentBuilder(context, null).build().writeToFileWithoutExtension(outputFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    /**
     * Builds the document returns it as a String.
     *
     * @return the document as a String
     * @throws java.io.IOException if files can not be read
     */
    public String asString() throws IOException {
        applySwaggerExtensions();
        
        StringBuilder sb = new StringBuilder();
        sb.append(new OverviewDocumentBuilder(context, null).build().toString());
        sb.append(new PathsDocumentBuilder(context, null).build().toString());
        sb.append(new DefinitionsDocumentBuilder(context, null).build().toString());
        sb.append(new SecurityDocumentBuilder(context, null).build().toString());
        return sb.toString();
    }

    private void applySwaggerExtensions() {
        for (SwaggerModelExtension swaggerModelExtension : context.extensionRegistry.getSwaggerModelExtensions()) {
            swaggerModelExtension.apply(context.getSwagger());
        }
    }

    public static class Builder {
        private final Swagger swagger;
        private final URI swaggerLocation;
        private Swagger2MarkupConfig config;
        private Swagger2MarkupExtensionRegistry extensionRegistry;

        /**
         * Creates a Builder from a remote URL.
         *
         * @param swaggerUrl the remote URL
         */
        Builder(URL swaggerUrl) {
            try {
                this.swaggerLocation = swaggerUrl.toURI();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("swaggerURL is in a wrong format", e);
            }
            this.swagger = readSwagger(swaggerUrl.toString());
        }

        /**
         * Creates a Builder from a local Path.
         *
         * @param swaggerPath the local Path
         */
        Builder(Path swaggerPath) {
            this.swaggerLocation = swaggerPath.toAbsolutePath().toUri();
            this.swagger = readSwagger(swaggerPath.toString());
        }

        /**
         * Creates a Builder using a given Swagger model.
         *
         * @param swagger the Swagger source.
         */
        Builder(Swagger swagger) {
            this.swagger = swagger;
            this.swaggerLocation = null;
        }

        /**
         * Uses the SwaggerParser to read the Swagger source.
         *
         * @param swaggerLocation the location of the Swagger source
         * @return the Swagger model
         */
        private Swagger readSwagger(String swaggerLocation){
            Swagger swagger = new SwaggerParser().read(swaggerLocation);
            if (swagger == null) {
                throw new IllegalArgumentException("Failed to read the Swagger source");
            }
            return swagger;
        }

        public Builder withConfig(Swagger2MarkupConfig config) {
            Validate.notNull(config, "config must not be null");
            this.config = config;
            return this;
        }

        public Builder withExtensionRegistry(Swagger2MarkupExtensionRegistry registry) {
            Validate.notNull(config, "registry must not be null");
            this.extensionRegistry = registry;
            return this;
        }

        public Swagger2MarkupConverter build() {
            if (config == null)
                config = new Swagger2MarkupConfigBuilder().build();

            if (extensionRegistry == null)
                extensionRegistry = new Swagger2MarkupExtensionRegistryBuilder().build();

            Context context = new Context(config, extensionRegistry, swagger, swaggerLocation);

            initExtensions(context);

            return new Swagger2MarkupConverter(context);
        }

        private void initExtensions(Context context) {
            for (SwaggerModelExtension extension : extensionRegistry.getSwaggerModelExtensions())
                extension.setGlobalContext(context);

            for (OverviewDocumentExtension extension : extensionRegistry.getOverviewDocumentExtensions())
                extension.setGlobalContext(context);

            for (DefinitionsDocumentExtension extension : extensionRegistry.getDefinitionsDocumentExtensions())
                extension.setGlobalContext(context);

            for (PathsDocumentExtension extension : extensionRegistry.getPathsDocumentExtensions())
                extension.setGlobalContext(context);

            for (SecurityDocumentExtension extension : extensionRegistry.getSecurityDocumentExtensions())
                extension.setGlobalContext(context);
        }
    }

    public static class Context {
        private Swagger2MarkupConfig config;
        private Swagger2MarkupExtensionRegistry extensionRegistry;
        private Swagger swagger;
        private URI swaggerLocation;

        Context(Swagger2MarkupConfig config, Swagger2MarkupExtensionRegistry extensionRegistry, Swagger swagger, URI swaggerLocation) {
            this.config = config;
            this.extensionRegistry = extensionRegistry;
            this.swagger = swagger;
            this.swaggerLocation = swaggerLocation;
        }

        public Swagger2MarkupConfig getConfig() {
            return config;
        }

        public Swagger2MarkupExtensionRegistry getExtensionRegistry() {
            return extensionRegistry;
        }

        public Swagger getSwagger() {
            return swagger;
        }

        public URI getSwaggerLocation() {
            return swaggerLocation;
        }
    }

}
