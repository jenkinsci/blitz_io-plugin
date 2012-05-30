/*
 * The MIT License
 *
 * Copyright 2012 Spirent Communications.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.blitz.jenkins;

import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import io.blitz.curl.AbstractTest;
import io.blitz.curl.Rush;
import io.blitz.curl.config.Pattern;
import io.blitz.jenkins.listener.RushListener;
import io.blitz.jenkins.listener.SprintListener;
import io.blitz.jenkins.util.JsonConverter;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;

/**
 * <p>
 * When the user configures the project and enables this publisher,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link BlitzIOPublisher} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #rush})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked.
 *
 * @author Jeff Li
 */
public class BlitzIOPublisher extends Recorder {

    private boolean runSprint = false;
    private boolean runRush = false;
    private String sprint;
    private String rush;
    private String responseTime;
    private String errorRate;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public BlitzIOPublisher(Boolean runSprint, String sprint, String responseTime,
        Boolean runRush, String rush, String errorRate) {
        this.runSprint = runSprint;
        this.runRush = runRush;

        if(runSprint){
            this.sprint = sprint.trim();
            this.responseTime = responseTime.trim();
        }

        if(runRush){
            this.rush = rush.trim();
            this.errorRate = errorRate.trim();
        }
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public boolean getRunSprint() {
        return runSprint;
    }

    public boolean getRunRush(){
        return runRush;
    }

    public String getSprint() {
        return sprint;
    }

    public String getResponseTime() {
        return responseTime;
    }

    public String getRush() {
        return rush;
    }

    public String getErrorRate() {
        return errorRate;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {

        final PrintStream logger = listener.getLogger();
        Exception ex = null;

        String username = getDescriptor().getUserID();
        String apiKey = getDescriptor().getKey();
        String serverHost = "blitz.io";
        Integer port = 80;
        String sprint = getSprint();
        String rush = getRush();
        String rushPattern = "";
        int respTime = 0, errRate = 0;

        try{
            respTime = Integer.parseInt(getResponseTime().trim());
            errRate = Integer.parseInt(getErrorRate().trim());
        }catch(Exception e){
            // default to 100 milliseconds for sprint and 5% error rate for rush
            respTime = 100;
            errRate = 5;
        }

//        System.out.println("username: "+username);
//        System.out.println("api key: "+apiKey);
//        System.out.println("run sprint: "+getRunSprint());
//        System.out.println("sprint: "+getSprint());
//        System.out.println("response time: "+respTime);
//
//        System.out.println("run rush: "+getRunRush());
//        System.out.println("rush: "+getRush());
//        System.out.println("error rate: "+errRate);

        RushListener rushLisener = new RushListener();
        SprintListener sprintLisener = new SprintListener();

        try {
            if(getRunSprint() && sprint.length() > 0 && respTime > 0){
                AbstractTest test = io.blitz.command.Curl.parse(username, apiKey,
                        serverHost, port, sprint);

                test.addListener(sprintLisener);
                test.execute();

                Thread.currentThread().sleep(2000); // wait 2 seconds for throttle
            }

            if(getRunRush() && rush.length() > 0 && errRate > 0){
                AbstractTest test = io.blitz.command.Curl.parse(username, apiKey,
                        serverHost, port, rush);
                Pattern pattern = ((Rush)test).getPattern();
                rushPattern = JsonConverter.toJson(pattern);
                test.addListener(rushLisener);

                test.execute();
            }
        } catch (Exception e) {
            ex = e;
            System.out.println(e.fillInStackTrace());
            build.setResult(Result.FAILURE);
        }

        BlitzBuildAction action = new BlitzBuildAction(build, sprintLisener.getSprintResult(),
                sprint, respTime, rushLisener.getRushResult(), rush, errRate, rushPattern,
                ex);
        build.getActions().add(action);

        if(ex == null){
            build.setResult(Result.SUCCESS);
            return true;
        }else{
            return false;
        }
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link BlitzIOPublisher}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/io/blitz/jenkins/BlitzIOPublisher/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        /**
         * To persist configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String userID;
        private String key;

        public DescriptorImpl(){
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'userID'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
       public FormValidation doCheckUserID(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.trim().length() == 0) {
                return FormValidation.error("Please enter the user ID");
            }
            userID = value;
            return FormValidation.ok();
        }

        public FormValidation doCheckKey(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.trim().length() == 0) {
                return FormValidation.error("Please enter the Api Key");
            }
            key = value;
            return FormValidation.ok();
        }

        public FormValidation doCheckResponseTime(@QueryParameter String value)
                throws IOException, ServletException {
            if(value.trim().length() > 0){
                int val = 0;
                try {
                    val = Integer.parseInt(value);
                } catch (Exception ex) {
                    return FormValidation.error("Please enter an integer.");
                }

                if (val <= 0) {
                    return FormValidation.error("The response time should be greater than 0.");
                }
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckErrorRate(@QueryParameter String value)
                throws IOException, ServletException {
            if(value.trim().length() > 0){
                int val = 0;
                try {
                    val = Integer.parseInt(value);
                } catch (Exception ex) {
                    return FormValidation.error("Please enter an integer.");
                }

                if (val < 0 || val > 100) {
                    return FormValidation.error("Error rate should be between 0 and 100.");
                }
            }
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Blitz.io";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            userID = formData.getString("userID").trim();
            key = formData.getString("key").trim();

            save();
            return super.configure(req,formData);
        }

        /**
         * These methods are called in
         * <tt>src/main/resources/io/blitz/jenkins/BlitzIOPublisher/global.jelly</tt>
         */
        public String getUserID() {
            return userID;
        }

        public String getKey() {
            return key;
        }
    }
}

