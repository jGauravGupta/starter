/*
 *
 * Copyright (c) 2024 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.starter.application.generator;

import fish.payara.starter.application.domain.ERModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that {@link CRUDAppGenerator} produces correct Jakarta Data repository
 * interfaces (Jakarta EE 11+) and correct legacy repository classes (Jakarta EE ≤ 10).
 */
class JakartaDataGeneratorTest {

    private static final String PACKAGE = "fish.payara.test";
    private static final String DOMAIN_LAYER = "domain";
    private static final String REPOSITORY_LAYER = "service";
    private static final String CONTROLLER_LAYER = "resource";

    private static final String ER_DIAGRAM = """
            erDiagram
                EMPLOYEE {
                    int employeeId PK
                    string name
                    string position
                }
            """;

    private File outputDir;

    @BeforeEach
    void setUp() throws IOException {
        outputDir = Files.createTempDirectory("jakarta-data-test-").toFile();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (outputDir != null && outputDir.exists()) {
            Files.walk(outputDir.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ERModel buildModel(double jakartaVersion) {
        ERDiagramParser parser = new ERDiagramParser();
        ERModel model = parser.parse(ER_DIAGRAM);
        model.setImportPrefix("jakarta");
        model.setJakartaVersion(jakartaVersion);
        return model;
    }

    private void generate(ERModel model, String generateWeb) throws IOException {
        CRUDAppGenerator generator = new CRUDAppGenerator(
                model, PACKAGE, DOMAIN_LAYER, REPOSITORY_LAYER, CONTROLLER_LAYER);
        generator.generate(outputDir, true, true, true, generateWeb);
    }

    /**
     * Resolves a generated Java file relative to {@code src/main/java} inside the output dir.
     */
    private File generatedFile(String subpackage, String fileName) {
        return new File(outputDir,
                "src/main/java/" + (PACKAGE + "." + subpackage).replace('.', '/') + "/" + fileName);
    }

    private String readFile(File file) throws IOException {
        return Files.readString(file.toPath());
    }

    // -----------------------------------------------------------------------
    // Jakarta EE 11 – Jakarta Data repository tests
    // -----------------------------------------------------------------------

    @Test
    void jakartaEE11_repositoryIsInterface() throws IOException {
        generate(buildModel(11), "html");

        File repoFile = generatedFile(REPOSITORY_LAYER, "EmployeeService.java");
        assertTrue(repoFile.exists(), "Repository file should be generated");

        String content = readFile(repoFile);
        assertTrue(content.contains("public interface EmployeeService"),
                "Repository should be a Java interface for Jakarta EE 11");
    }

    @Test
    void jakartaEE11_repositoryHasRepositoryAnnotation() throws IOException {
        generate(buildModel(11), "html");

        String content = readFile(generatedFile(REPOSITORY_LAYER, "EmployeeService.java"));
        assertTrue(content.contains("@Repository"),
                "Repository should carry the @Repository annotation");
    }

    @Test
    void jakartaEE11_repositoryExtendsCrudRepository() throws IOException {
        generate(buildModel(11), "html");

        String content = readFile(generatedFile(REPOSITORY_LAYER, "EmployeeService.java"));
        assertTrue(content.contains("extends CrudRepository<Employee, Integer>"),
                "Repository should extend CrudRepository with entity and PK type parameters");
    }

    @Test
    void jakartaEE11_repositoryImportsCrudRepository() throws IOException {
        generate(buildModel(11), "html");

        String content = readFile(generatedFile(REPOSITORY_LAYER, "EmployeeService.java"));
        assertTrue(content.contains("import jakarta.data.repository.CrudRepository"),
                "Repository should import jakarta.data.repository.CrudRepository");
    }

    @Test
    void jakartaEE11_repositoryImportsRepositoryAnnotation() throws IOException {
        generate(buildModel(11), "html");

        String content = readFile(generatedFile(REPOSITORY_LAYER, "EmployeeService.java"));
        assertTrue(content.contains("import jakarta.data.repository.Repository"),
                "Repository should import jakarta.data.repository.Repository");
    }

    @Test
    void jakartaEE11_noAbstractRepositoryGenerated() throws IOException {
        generate(buildModel(11), "html");

        File abstractRepo = generatedFile(REPOSITORY_LAYER, "AbstractService.java");
        assertFalse(abstractRepo.exists(),
                "AbstractService.java should NOT be generated for Jakarta EE 11");
    }

    // -----------------------------------------------------------------------
    // Jakarta EE 11 – REST controller uses Jakarta Data methods
    // -----------------------------------------------------------------------

    @Test
    void jakartaEE11_restControllerUsesSaveForCreate() throws IOException {
        generate(buildModel(11), "html");

        String content = readFile(generatedFile(CONTROLLER_LAYER, "EmployeeResource.java"));
        assertTrue(content.contains("employeeService.save("),
                "REST controller create method should call repository.save() for Jakarta EE 11");
        assertFalse(content.contains("employeeService.create("),
                "REST controller should NOT call repository.create() for Jakarta EE 11");
    }

    @Test
    void jakartaEE11_restControllerUsesSaveForUpdate() throws IOException {
        generate(buildModel(11), "html");

        String content = readFile(generatedFile(CONTROLLER_LAYER, "EmployeeResource.java"));
        assertFalse(content.contains("employeeService.edit("),
                "REST controller should NOT call repository.edit() for Jakarta EE 11");
    }

    @Test
    void jakartaEE11_restControllerUsesFindById() throws IOException {
        generate(buildModel(11), "html");

        String content = readFile(generatedFile(CONTROLLER_LAYER, "EmployeeResource.java"));
        assertTrue(content.contains("employeeService.findById("),
                "REST controller get method should call repository.findById() for Jakarta EE 11");
        assertFalse(content.contains("employeeService.find("),
                "REST controller should NOT call repository.find() for Jakarta EE 11");
    }

    @Test
    void jakartaEE11_restControllerUsesDeleteById() throws IOException {
        generate(buildModel(11), "html");

        String content = readFile(generatedFile(CONTROLLER_LAYER, "EmployeeResource.java"));
        assertTrue(content.contains("employeeService.deleteById("),
                "REST controller delete method should call repository.deleteById() for Jakarta EE 11");
        assertFalse(content.contains("employeeService.remove("),
                "REST controller should NOT call repository.remove() for Jakarta EE 11");
    }

    // -----------------------------------------------------------------------
    // Jakarta EE 11 – JSF bean uses Jakarta Data methods
    // -----------------------------------------------------------------------

    @Test
    void jakartaEE11_jsfBeanUsesSave() throws IOException {
        generate(buildModel(11), "jsf");

        String content = readFile(generatedFile(CONTROLLER_LAYER, "EmployeeBean.java"));
        assertTrue(content.contains("employeeService.save("),
                "JSF bean save method should call repository.save() for Jakarta EE 11");
        assertFalse(content.contains("employeeService.create("),
                "JSF bean should NOT call repository.create() for Jakarta EE 11");
        assertFalse(content.contains("employeeService.edit("),
                "JSF bean should NOT call repository.edit() for Jakarta EE 11");
    }

    @Test
    void jakartaEE11_jsfBeanUsesDeleteById() throws IOException {
        generate(buildModel(11), "jsf");

        String content = readFile(generatedFile(CONTROLLER_LAYER, "EmployeeBean.java"));
        assertTrue(content.contains("employeeService.deleteById("),
                "JSF bean remove method should call repository.deleteById() for Jakarta EE 11");
        assertFalse(content.contains("employeeService.remove("),
                "JSF bean should NOT call repository.remove() for Jakarta EE 11");
    }

    // -----------------------------------------------------------------------
    // Legacy (Jakarta EE 10) – AbstractRepository and classic methods
    // -----------------------------------------------------------------------

    @Test
    void legacyEE10_abstractRepositoryGenerated() throws IOException {
        generate(buildModel(10), "html");

        File abstractRepo = generatedFile(REPOSITORY_LAYER, "AbstractService.java");
        assertTrue(abstractRepo.exists(),
                "AbstractService.java should be generated for Jakarta EE 10");
    }

    @Test
    void legacyEE10_repositoryIsClass() throws IOException {
        generate(buildModel(10), "html");

        String content = readFile(generatedFile(REPOSITORY_LAYER, "EmployeeService.java"));
        assertTrue(content.contains("public class EmployeeService"),
                "Repository should be a Java class for Jakarta EE 10");
    }

    @Test
    void legacyEE10_repositoryExtendsAbstractRepository() throws IOException {
        generate(buildModel(10), "html");

        String content = readFile(generatedFile(REPOSITORY_LAYER, "EmployeeService.java"));
        assertTrue(content.contains("extends AbstractService"),
                "Legacy repository should extend the generated AbstractService");
    }

    @Test
    void legacyEE10_restControllerUsesCreateAndEdit() throws IOException {
        generate(buildModel(10), "html");

        String content = readFile(generatedFile(CONTROLLER_LAYER, "EmployeeResource.java"));
        assertTrue(content.contains("employeeService.create("),
                "Legacy REST controller should call repository.create() for Jakarta EE 10");
        assertTrue(content.contains("employeeService.edit("),
                "Legacy REST controller should call repository.edit() for Jakarta EE 10");
        assertFalse(content.contains("employeeService.save("),
                "Legacy REST controller should NOT call repository.save() for Jakarta EE 10");
    }

    @Test
    void legacyEE10_restControllerUsesFindAndRemove() throws IOException {
        generate(buildModel(10), "html");

        String content = readFile(generatedFile(CONTROLLER_LAYER, "EmployeeResource.java"));
        assertTrue(content.contains("employeeService.find("),
                "Legacy REST controller should call repository.find() for Jakarta EE 10");
        assertTrue(content.contains("employeeService.remove("),
                "Legacy REST controller should call repository.remove() for Jakarta EE 10");
        assertFalse(content.contains("employeeService.findById("),
                "Legacy REST controller should NOT call repository.findById() for Jakarta EE 10");
        assertFalse(content.contains("employeeService.deleteById("),
                "Legacy REST controller should NOT call repository.deleteById() for Jakarta EE 10");
    }

    // -----------------------------------------------------------------------
    // Bug-fix: Converter – .find() vs .findById().orElse(null)
    // -----------------------------------------------------------------------

    @Test
    void jakartaEE11_converterUsesFindById() throws IOException {
        generate(buildModel(11), "jsf");

        String content = readFile(generatedFile("converter", "EmployeeConverter.java"));
        assertTrue(content.contains(".findById("),
                "Converter should call findById() for Jakarta EE 11");
        assertTrue(content.contains(".orElse(null)"),
                "Converter findById() result should use .orElse(null) for Jakarta EE 11");
        assertFalse(content.contains("employeeService.find("),
                "Converter should NOT call find() for Jakarta EE 11");
    }

    @Test
    void legacyEE10_converterUsesFind() throws IOException {
        generate(buildModel(10), "jsf");

        String content = readFile(generatedFile("converter", "EmployeeConverter.java"));
        assertTrue(content.contains("employeeService.find("),
                "Converter should call find() for Jakarta EE 10");
        assertFalse(content.contains(".findById("),
                "Converter should NOT call findById() for Jakarta EE 10");
    }

    // -----------------------------------------------------------------------
    // Bug-fix: JSF bean – findAll() returns Stream in Jakarta Data
    // -----------------------------------------------------------------------

    @Test
    void jakartaEE11_jsfBeanFindAllConvertsToList() throws IOException {
        generate(buildModel(11), "jsf");

        String content = readFile(generatedFile(CONTROLLER_LAYER, "EmployeeBean.java"));
        assertTrue(content.contains("findAll().toList()"),
                "JSF bean getAll method should call findAll().toList() for Jakarta EE 11");
    }

    @Test
    void legacyEE10_jsfBeanFindAllReturnsList() throws IOException {
        generate(buildModel(10), "jsf");

        String content = readFile(generatedFile(CONTROLLER_LAYER, "EmployeeBean.java"));
        assertFalse(content.contains("findAll().toList()"),
                "Legacy JSF bean should NOT call findAll().toList() for Jakarta EE 10");
        assertTrue(content.contains("findAll()"),
                "Legacy JSF bean should call findAll() directly for Jakarta EE 10");
    }

    // -----------------------------------------------------------------------
    // Bug-fix: REST controller – findAll() returns Stream in Jakarta Data
    // -----------------------------------------------------------------------

    @Test
    void jakartaEE11_restControllerFindAllConvertsToList() throws IOException {
        generate(buildModel(11), "html");

        String content = readFile(generatedFile(CONTROLLER_LAYER, "EmployeeResource.java"));
        assertTrue(content.contains("findAll().toList()"),
                "REST controller getAll method should call findAll().toList() for Jakarta EE 11");
    }

    @Test
    void legacyEE10_restControllerFindAllReturnsList() throws IOException {
        generate(buildModel(10), "html");

        String content = readFile(generatedFile(CONTROLLER_LAYER, "EmployeeResource.java"));
        assertFalse(content.contains("findAll().toList()"),
                "Legacy REST controller should NOT call findAll().toList() for Jakarta EE 10");
        assertTrue(content.contains("findAll()"),
                "Legacy REST controller should call findAll() directly for Jakarta EE 10");
    }
}
