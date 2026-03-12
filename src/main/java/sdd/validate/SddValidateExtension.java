package sdd.validate;

import java.util.ArrayList;
import java.util.List;

public class SddValidateExtension {
    private String specsDir = "specs";
    private String stepsDir = "src/test/java";
    private boolean failOnDivergence = true;
    private List<String> includeDomains = new ArrayList<>();
    private List<String> excludeDomains = new ArrayList<>();

    public String getSpecsDir() { return specsDir; }
    public void setSpecsDir(String specsDir) { this.specsDir = specsDir; }
    public String getStepsDir() { return stepsDir; }
    public void setStepsDir(String stepsDir) { this.stepsDir = stepsDir; }
    public boolean isFailOnDivergence() { return failOnDivergence; }
    public void setFailOnDivergence(boolean fail) { this.failOnDivergence = fail; }
    public List<String> getIncludeDomains() { return includeDomains; }
    public void setIncludeDomains(List<String> includeDomains) { this.includeDomains = includeDomains; }
    public List<String> getExcludeDomains() { return excludeDomains; }
    public void setExcludeDomains(List<String> excludeDomains) { this.excludeDomains = excludeDomains; }
}
