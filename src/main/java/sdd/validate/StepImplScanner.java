package sdd.validate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class StepImplScanner {

    private static final Pattern STEP_ANNOTATION = Pattern.compile("@Step\\(\"([^\"]+)\"\\)");

    public Set<String> scan(Path stepsDir) throws IOException {
        Set<String> patterns = new HashSet<>();

        List<Path> javaFiles;
        try (Stream<Path> stream = Files.walk(stepsDir)) {
            javaFiles = stream.filter(p -> p.toString().endsWith(".java")).toList();
        }

        for (Path javaFile : javaFiles) {
            String content = Files.readString(javaFile);
            Matcher matcher = STEP_ANNOTATION.matcher(content);
            while (matcher.find()) {
                patterns.add(matcher.group(1));
            }
        }
        return patterns;
    }
}
