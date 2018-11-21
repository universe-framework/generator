package eu.lpinto.universe.generator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Luis Pinto <code>- luis.pinto@petuniversal.com</code>
 */
public class Generator {

    static public void main(String[] args) throws IOException {
        String baseDirectory = null; // Set this you what to hardcode the project base directory path
        List<String> filesToSkip = null;

        /*
         * use the parameter if it exists
         */
        if (args.length == 1) {
            baseDirectory = args[0];

        } else if (baseDirectory == null) {
            System.out.println("Usage: pass a parametter with a path to explore or set it hardcoded in this function.");
            return;
        }

        if (baseDirectory.endsWith("/") == false) {
            baseDirectory += "/";
        }

        /*
         * Universe config file
         */
        File userHome = new File(System.getProperty("user.home") + "/" + UniverseConfiguration.UNIVERSE_FOLDER);
        File props = new File(userHome.getCanonicalPath() + "/" + UniverseConfiguration.FILE_NAME);

        if (userHome.exists() && props.exists()) {
            UniverseConfiguration properties = new UniverseConfiguration(props.getCanonicalPath());
            filesToSkip = properties.getGeneratorFilesToSlip();
        } else {
            userHome.mkdirs();
        }
        if (filesToSkip == null) {
            filesToSkip = new ArrayList<>(0);
        }

        System.out.println("Will skip:");
        for (String s : filesToSkip) {
            System.out.println("    " + s);
        }

        /*
         * Exploration
         */
        List<ClassInfo> classesInfo = new ArrayList<>(20);
        exploreDirectory(baseDirectory, classesInfo);

        System.out.println("Created:");

        for (ClassInfo classInfo : classesInfo) {
            File base = new File(classInfo.path).getParentFile().getParentFile().getParentFile();
            String basePackage = classInfo.path.split("java/")[1].split("/persistence")[0].replaceAll("/", ".");

            if (filesToSkip.contains(basePackage + ".persistence.entities." + classInfo.getName())) {
                continue; // if is declared in the config file to be skiped
            }

            /* Facade */
            printFacade(base, basePackage, classInfo);

            /* Controller */
            printController(base, basePackage, classInfo);

            /* DTO */
            printDTO(base, basePackage, classInfo);

            /* DTS */
            printDTS(base, basePackage, classInfo);

            /* Service */
            printService(base, basePackage, classInfo);
        }
    }

    static private void exploreDirectory(String dirPath, List<ClassInfo> classesInfo) throws IOException {
        if (dirPath.contains("/.")) {
            return;
        }

        File directory = new File(dirPath);
        String[] files = directory.list();

        if (files != null) {
            for (String fileName : files) {

                File aux = new File(dirPath + fileName);

                if (aux.isDirectory()) {
                    exploreDirectory(dirPath + fileName + "/", classesInfo);

                } else if (aux.isFile() && aux.getName().endsWith(".java")) {
                    ClassInfo buildClassInfo = buildClassInfo(dirPath + fileName);

                    if (buildClassInfo != null) {
                        classesInfo.add(buildClassInfo);
                    }
                }
            }
        }
    }

    static private ClassInfo buildClassInfo(String path) throws IOException {
        ClassInfo classInfo = new ClassInfo();
        Path pathObj = Paths.get(path);
        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(pathObj), charset);
        if (content.contains("@Entity") == false) {
            return null;
        }

        classInfo.path = path;

        try {
            Files.lines(Paths.get(path)).forEach(line -> {

                if (line.contains("class ")) {
                    classInfo.setName(line.split("class ")[1].split(" ")[0]);
                }

                if (line.matches("[\\s]*private [a-zA-Z<>]+ [a-zA-Z0-9]+;[\\s]*")) {
                    FieldInfo fieldInfo = new FieldInfo();
                    fieldInfo.setName(line.split("private [a-zA-Z<>]+ ")[1].split("(;| )")[0]);
                    fieldInfo.setType(line.split("private ")[1].split(" ")[0]);
                    classInfo.fields.add(fieldInfo);
                }
            });
        } catch (IOException | UncheckedIOException ex) {
            System.out.println("Cannot parse file: " + path);
        }

        return classInfo;
    }

    /*
     * Facades
     */
    private static void printFacade(File baseDir, String basePackage, ClassInfo classInfo) throws IOException, FileNotFoundException {
        File dir = new File(baseDir + "/persistence/facades/");

        if (dir.exists() == false) {
            dir.mkdirs();
        }

        File newFile = new File(dir.getCanonicalPath() + "/" + classInfo.getName() + "Facade.java");

        if (newFile.exists() == false) {
            Writer writer;
            writer = new OutputStreamWriter(new FileOutputStream(newFile.getAbsoluteFile()), StandardCharsets.UTF_8);
            String content = buildFacade(basePackage, classInfo);
            writer.write(content);
            writer.close();
            System.out.println("    " + newFile.getCanonicalPath());
        }
    }

    static private String buildFacade(String classPathStr, ClassInfo classInfo) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir") + "/src/main/templates/facade");
        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(path), charset);
        content = content.replaceFirst("/\\*([^/]|\n)*\\*/\n", "");
        content = content.replaceAll("\\$\\{package\\}", classPathStr + ".persistence.facades");
        content = content.replaceAll("\\$\\{entityPackage\\}", classPathStr + ".persistence.entities");
        content = content.replaceAll("\\$\\{name\\}", classInfo.getName());

        return content;
    }

    /*
     * Controllers
     */
    private static void printController(File baseDir, String basePackage, ClassInfo classInfo) throws IOException, FileNotFoundException {
        File dir = new File(baseDir + "/controllers/");

        if (dir.exists() == false) {
            dir.mkdirs();
        }

        File newFile = new File(dir.getCanonicalPath() + "/" + classInfo.getName() + "Controller.java");

        if (newFile.exists() == false) {
            Writer writer;
            writer = new OutputStreamWriter(new FileOutputStream(newFile.getAbsoluteFile()), StandardCharsets.UTF_8);
            String content = buildController(basePackage, classInfo);
            writer.write(content);
            writer.close();
            System.out.println("    " + newFile.getCanonicalPath());
        }
    }

    static private String buildController(String classPathStr, ClassInfo classInfo) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir") + "/src/main/templates/controller");
        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(path), charset);
        content = content.replaceFirst("/\\*([^/]|\n)*\\*/\n", "");
        content = content.replaceAll("\\$\\{package\\}", classPathStr + ".controllers");
        content = content.replaceAll("\\$\\{entityPackage\\}", classPathStr + ".persistence.entities");
        content = content.replaceAll("\\$\\{facadePackage\\}", classPathStr + ".persistence.facades");
        content = content.replaceAll("\\$\\{name\\}", classInfo.getName());

        return content;
    }

    /*
     * DTO
     */
    private static void printDTO(File baseDir, String basePackage, ClassInfo classInfo) throws IOException, FileNotFoundException {
        File dir = new File(baseDir + "/api/dto/");

        if (dir.exists() == false) {
            dir.mkdirs();
        }

        File newFile = new File(dir.getCanonicalPath() + "/" + classInfo.getName() + "DTO.java");

        if (newFile.exists() == false) {
            Writer writer;
            writer = new OutputStreamWriter(new FileOutputStream(newFile.getAbsoluteFile()), StandardCharsets.UTF_8);
            String content = buildDTO(basePackage, classInfo);
            writer.write(content);
            writer.close();
            System.out.println("    " + newFile.getCanonicalPath());
        }
    }

    static private String buildDTO(String classPathStr, ClassInfo classInfo) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir") + "/src/main/templates/dto");
        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(path), charset);
        content = content.replaceFirst("/\\*([^/]|\n)*\\*/\n", "");
        content = content.replaceAll("\\$\\{package\\}", classPathStr + ".api.dto");
        content = content.replaceAll("\\$\\{name\\}", classInfo.getName());
        content = content.replaceAll("\\$\\{fields\\}", buildFields(classInfo));
        content = content.replaceAll("\\$\\{constructor\\}", buildConstructor(classInfo));
        content = content.replaceAll("\\$\\{methods\\}", buildMethods(classInfo));

        return content;
    }

    static private String buildFields(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder(300);
        for (FieldInfo fieldInfo : classInfo.fields) {
            sb.append("    private ").append(fieldInfo.getDtoType()).append(" ").append(fieldInfo.getName()).append(";\n");
        }
        return sb.toString();
    }

    static private String buildConstructor(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder(300);

        sb.append("    public ").append(classInfo.getName()).append("DTO(");

        for (int i = 0; i < classInfo.fields.size(); i++) {
            FieldInfo fieldInfo = classInfo.fields.get(i);
            sb.append("final ").append(fieldInfo.getDtoType()).append(" ").append(fieldInfo.getName()).append(", ");
        }
        sb.append(String.format("\n            %1$-" + (classInfo.getName() + "DTO").length() + "s", " "))
                .append("final String name, final Long creator, final Calendar created,\n")
                .append(String.format("            %1$-" + (classInfo.getName() + "DTO").length() + "s", " "))
                .append("final Long updater, final Calendar updated, final Long id) {\n        super(name, creator, created, updater, updated, id);\n");

        for (FieldInfo fieldInfo : classInfo.fields) {
            sb.append("        this.").append(fieldInfo.getName()).append(" = ").append(fieldInfo.getName()).append(";\n");
        }
        sb.append("    }\n");

        return sb.toString();
    }

    static private String buildMethods(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder(300);
        for (FieldInfo fieldInfo : classInfo.fields) {
            sb.append("\n    public ").append(fieldInfo.getDtoType()).append(" get").append(toUpperCamelCase(fieldInfo.getName())).append("() {\n");
            sb.append("        return this.").append(fieldInfo.getName()).append(";\n");
            sb.append("    }\n");

            sb.append("\n    public void set").append(toUpperCamelCase(fieldInfo.getName())).append("(final ").append(fieldInfo.getDtoType()).append(" ").append(fieldInfo.getName())
                    .append(") {\n");
            sb.append("        this.").append(fieldInfo.getName()).append(" = ").append(fieldInfo.getName()).append(";\n");
            sb.append("    }\n");
        }
        return sb.toString();
    }

    private static String toUpperCamelCase(final String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private static String toLowerCamelCase(final String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    /*
     * DTS
     */
    private static void printDTS(File baseDir, String basePackage, ClassInfo classInfo) throws IOException, FileNotFoundException {
        File dir = new File(baseDir + "/api/dts/");

        if (dir.exists() == false) {
            dir.mkdirs();
        }

        File newFile = new File(dir.getCanonicalPath() + "/" + classInfo.getName() + "DTS.java");

        if (newFile.exists() == false) {
            Writer writer;
            writer = new OutputStreamWriter(new FileOutputStream(newFile.getAbsoluteFile()), StandardCharsets.UTF_8);
            String content = buildDTS(basePackage, classInfo);
            writer.write(content);
            writer.close();
            System.out.println("    " + newFile.getCanonicalPath());
        }
    }

    static private String buildDTS(String classPathStr, ClassInfo classInfo) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir") + "/src/main/templates/dts");
        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(path), charset);
        content = content.replaceFirst("/\\*([^/]|\n)*\\*/\n", "");
        content = content.replaceAll("\\$\\{basePackage\\}", classPathStr);
        content = content.replaceAll("\\$\\{name\\}", classInfo.getName());
        content = content.replaceAll("\\$\\{toApiFull\\}", buildDtsToApiFull(classInfo));
        content = content.replaceAll("\\$\\{toApiNotFull\\}", buildDtsToApiNotFull(classInfo));
        content = content.replaceAll("\\$\\{toDomain\\}", buildDtsToDomain(classInfo));

        return content;
    }

    static private String buildDtsToApiFull(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder(300);

        for (int i = 0; i < classInfo.fields.size(); i++) {
            FieldInfo fieldInfo = classInfo.fields.get(i);
            sb.append("\n                    ");

            if (fieldInfo.getType().equals(fieldInfo.getDtoType())) {
                // not FK
                sb.append("entity.get").append(toUpperCamelCase(fieldInfo.getName())).append("(),\n");
            } else {
                // FK
                sb.append(toUpperCamelCase(fieldInfo.getType())).append("DTS.id(entity.get").append(toUpperCamelCase(fieldInfo.getType())).append("()),");
            }

        }

        return sb.toString();
    }

    static private String buildDtsToApiNotFull(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder(300);

        for (int i = 0; i < classInfo.fields.size(); i++) {
            FieldInfo fieldInfo = classInfo.fields.get(i);
            sb.append("\n                    ");

            if (fieldInfo.getType().equals(fieldInfo.getDtoType())) {
                // not FK
                sb.append("entity.get").append(toUpperCamelCase(fieldInfo.getName())).append("(),\n");
            } else {
                // FK
                sb.append("null, // ").append(fieldInfo.getName());
            }

        }

        return sb.toString();
    }

    static private String buildDtsToDomain(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder(300);

        for (int i = 0; i < classInfo.fields.size(); i++) {
            FieldInfo fieldInfo = classInfo.fields.get(i);
            sb.append("\n                ");

            if (fieldInfo.getType().equals(fieldInfo.getDtoType())) {
                // not FK
                sb.append("dto.get").append(toUpperCamelCase(fieldInfo.getName())).append("(),\n");
            } else {
                // FK
                sb.append(toUpperCamelCase(fieldInfo.getType())).append("DTS.T.toDomain(dto.get").append(toUpperCamelCase(fieldInfo.getType())).append("()),");
            }

        }

        return sb.toString();
    }

    /*
     * Service
     */
    private static void printService(File baseDir, String basePackage, ClassInfo classInfo) throws IOException, FileNotFoundException {
        File dir = new File(baseDir + "/api/services/");

        if (dir.exists() == false) {
            dir.mkdirs();
        }

        File newFile = new File(dir.getCanonicalPath() + "/" + classInfo.getName() + "Service.java");

        if (newFile.exists() == false) {
            Writer writer;
            writer = new OutputStreamWriter(new FileOutputStream(newFile.getAbsoluteFile()), StandardCharsets.UTF_8);
            String content = buildService(basePackage, classInfo);
            writer.write(content);
            writer.close();
            System.out.println("    " + newFile.getCanonicalPath());
        }
    }

    static private String buildService(String classPathStr, ClassInfo classInfo) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir") + "/src/main/templates/service");
        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(path), charset);
        content = content.replaceFirst("/\\*([^/]|\n)*\\*/\n", "");
        content = content.replaceAll("\\$\\{basePackage\\}", classPathStr);
        content = content.replaceAll("\\$\\{name\\}", classInfo.getName());
        content = content.replaceAll("\\$\\{endpoint\\}", toLowerCamelCase(classInfo.getName() + "s"));

        return content;
    }

    /*
     * Helper classes
     */
    static private class FieldInfo {

        private String dtoType;
        private String type;
        private String name;

        public String getDtoType() {
            return dtoType;
        }

        public void setDtoType(String dtoType) {
            this.dtoType = dtoType;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {

            if (type.startsWith("List")) {
                this.dtoType = "List<Long>";
                this.type = type;

            } else if (Arrays.asList("Integer", "Long", "Double", "Float", "Calendar").contains(type)) {
                this.type = type;

            } else {
                this.dtoType = "Long";
                this.type = type;
            }
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static private class ClassInfo {

        public String name;
        public String path;
        List<FieldInfo> fields = new ArrayList<>(5);

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public List<FieldInfo> getFields() {
            return fields;
        }

        public void setFields(List<FieldInfo> fields) {
            this.fields = fields;
        }
    }
}
