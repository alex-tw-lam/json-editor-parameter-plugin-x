package io.jenkins.plugins.json_editor_parameter;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.ParameterValue;
import hudson.model.SimpleParameterDefinition;
import hudson.util.FormValidation;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import jenkins.model.Jenkins;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.verb.POST;

@EqualsAndHashCode(callSuper = true)
@Getter
public class JsonEditorParameterDefinition extends SimpleParameterDefinition {

    private static final Pattern OK_NAME = Pattern.compile("[A-Za-z][\\w-]{0,63}");

    private String schema;
    private String json;
    private String options = "{}";

    @DataBoundConstructor
    public JsonEditorParameterDefinition(String name) {
        super(name);
        if (!isValidName(name)) {
            throw new IllegalArgumentException("Invalid Name - " + name);
        }
    }

    private static void checkValidJson(String json, String errorMessage) {
        try {
            JSONSerializer.toJSON(json);
        } catch (JSONException e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private static boolean isValidName(String name) {
        return OK_NAME.matcher(name).matches();
    }

    @DataBoundSetter
    public void setSchema(String schema) {
        checkValidJson(schema, "schema must be valid json");
        this.schema = schema;
    }

    @DataBoundSetter
    public void setJson(String json) {
        json = Util.fixEmptyAndTrim(json);
        if (json != null) {
            checkValidJson(json, "json must be valid json or empty.");
        }
        this.json = json;
    }

    @DataBoundSetter
    public void setOptions(String options) {
        checkValidJson(options, "options must be valid json");
        this.options = options;
    }

    @Restricted(DoNotUse.class) // invoked from index.jelly
    public String getMergedOptions() {
        Map<String, Object> optionMap = new HashMap<>();

        // Add schema (always present as it's mandatory)
        if (schema != null && !schema.isEmpty()) {
            optionMap.put("schema", JsonUtil.toMap(schema));
        }

        // Add json if provided
        if (json != null && !json.isEmpty()) {
            optionMap.put("json", JsonUtil.toObject(json));
        }

        String result = JsonUtil.toJson(optionMap);
        System.out.println("Merged Options: " + result); // Debug output
        return result;
    }

    @Override
    public JsonEditorParameterDefinition copyWithDefaultValue(ParameterValue defaultValue) {
        if (defaultValue instanceof JsonEditorParameterValue) {
            JsonEditorParameterDefinition def = new JsonEditorParameterDefinition(getName());
            def.setDescription(getDescription());
            def.setJson(((JsonEditorParameterValue) defaultValue).getJson());
            return def;
        } else {
            return this;
        }
    }

    @Override
    public JsonEditorParameterValue createValue(StaplerRequest request, JSONObject jo) {
        JsonEditorParameterValue value = request.bindJSON(JsonEditorParameterValue.class, jo);
        value.setDescription(getDescription());
        return value;
    }

    public JsonEditorParameterValue createValue(String json) {
        return new JsonEditorParameterValue(getName(), json, getDescription());
    }

    @Override
    public JsonEditorParameterValue getDefaultParameterValue() {
        if (json == null) {
            return null;
        }
        return new JsonEditorParameterValue(getName(), json, getDescription());
    }

    @Extension
    @Symbol({"jsonEditor"})
    public static class DescriptorImpl extends ParameterDescriptor implements DescriptorChecks {

        private static FormValidation isValidJson(String options, String errorMessage) {
            try {
                checkValidJson(options, errorMessage);
                return FormValidation.ok();
            } catch (IllegalArgumentException e) {
                return FormValidation.error(errorMessage);
            }
        }

        @Override
        @POST
        public FormValidation doCheckName(@QueryParameter String name) {
            Jenkins.get().checkPermission(Jenkins.READ);
            return isValidName(name)
                    ? FormValidation.ok()
                    : FormValidation.error("Name should match regular expression [A-Za-z][\\w-]{0,63}");
        }

        @Override
        @POST
        public FormValidation doCheckOptions(@QueryParameter String options) {
            Jenkins.get().checkPermission(Jenkins.READ);
            return isValidJson(options, "options must be valid json");
        }

        @Override
        @POST
        public FormValidation doCheckSchema(@QueryParameter String schema) {
            Jenkins.get().checkPermission(Jenkins.READ);
            return isValidJson(schema, "schema must be valid json");
        }

        @Override
        @POST
        public FormValidation doCheckJson(@QueryParameter String json) {
            Jenkins.get().checkPermission(Jenkins.READ);
            if (Util.fixEmptyAndTrim(json) == null) {
                return FormValidation.ok();
            }
            return isValidJson(json, "json must be valid json or empty");
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return "Json Editor Parameter Definition";
        }
    }
}
