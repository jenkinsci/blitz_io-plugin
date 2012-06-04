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

import hudson.model.AbstractBuild;
import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import hudson.model.Result;
import io.blitz.curl.rush.Point;
import io.blitz.curl.rush.RushResult;
import io.blitz.curl.sprint.Step;
import io.blitz.curl.sprint.SprintResult;
import io.blitz.jenkins.util.JsonConverter;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * @author jeffli
 */
public class BlitzBuildAction implements HealthReportingAction{

    protected final AbstractBuild<?, ?> owner;
    private SprintResult sprintResult;
    private String sprint;
    private int responseTime;
    private RushResult rushResult;
    private String rush;
    private String rushPattern;
    private int errorRate;
    private Exception testException;

    /**
     * Constructor
     */
    public BlitzBuildAction(AbstractBuild<?, ?> owner, SprintResult sprintResult,
            String sprint, int responseTime, RushResult rushResult, String rush,
            int errorRate, String rushPattern, Exception testException){

        this.owner = owner;
        this.sprintResult = sprintResult;
        this.sprint = sprint;
        this.rushResult = rushResult;
        this.rush = rush;
        this.rushPattern = rushPattern;
        this.errorRate = errorRate;
        this.responseTime = responseTime;
        this.testException = testException;
    }

    public HealthReport getBuildHealth() {
        return null;
    }

    public <T extends BlitzBuildAction> T getPreviousResult() {
        AbstractBuild<?, ?> b = owner;
        while (true) {
            b = b.getPreviousBuild();
            if (b == null)
                return null;
            if (b.getResult() == Result.FAILURE)
                continue;
            BlitzBuildAction r = b.getAction(this.getClass());
            if (r != null)
                return (T) r;
        }
    }

    public AbstractBuild<?, ?> getOwner() {
        return owner;
    }

    public String getIconFileName() {
        return "/plugin/blitz_io-jenkins/images/16x16/blitz.png";
    }


    public String getDisplayName() {
        return "Blitz.io";
    }

    public String getUrlName() {
        return "blitz";
    }

    /**
     * @return the rushResult
     */
    public RushResult getRushResult() {
        return rushResult;
    }

    public String getRushResultJson() {
        return JsonConverter.toJson(rushResult);
    }

    /**
     * @return the errorRate
     */
    public int getErrorRate() {
        return errorRate;
    }

    /**
     * @return the sprintResult
     */
    public SprintResult getSprintResult() {
        return sprintResult;
    }

    /**
     * @return the responseTime
     */
    public int getResponseTime() {
        return responseTime;
    }

    public String getSprint(){
        return sprint;
    }

    public String getRush(){
        return rush;
    }

    public String getRushPattern(){
        return rushPattern;
    }

    /**
     * @return the testException
     */
    public Exception getTestException() {
        return testException;
    }

    public long getSprintResponseTime(){
        long duration = 0;
        SprintResult result = getSprintResult();

        if(result != null){
            duration = Math.round(result.getDuration()*1000);
        }
        return duration;
    }

    public long getSprintConnectionTime(){
        long connect = 0;
        SprintResult result = getSprintResult();

        if(result != null){
            ArrayList<Step> steps = (ArrayList) result.getSteps();

            if (steps != null && steps.size() > 0) {
                Step step = (Step) steps.get(steps.size() - 1);

                connect = Math.round(step.getConnect()*1000);
            }
        }
        return connect;
    }

    /*
     * if the sprint test has failed
     */
    public boolean isSprintTestFailed(){
        return getSprint()!=null && getSprint().length()>0
               && getSprintResponseTime()>getResponseTime();
    }

    public double getRushErrorRate(){
        double errRate = 0;
        RushResult result = getRushResult();

        if(result != null){
            ArrayList timeline = (ArrayList) result.getTimeline();
            if (timeline != null && timeline.size() > 0) {
                Point p = (Point) timeline.get(timeline.size() - 1);

                int hits = p.getHits();
                int errors = p.getErrors();
                int timeouts = p.getTimeouts();

                errRate = (errors + timeouts) * 100.0 / (errors + timeouts + hits);
            }
        }
        return errRate;
    }

    public String getRushErrorRateFormatted(){
        double errRate = getRushErrorRate();

        if(errRate > 0){
            DecimalFormat myFormatter = new DecimalFormat("0.###");
            return myFormatter.format(errRate);
        }
        return "0";
    }

    /*
     * if the rush test has failed
     */
    public boolean isRushTestFailed(){
        return getRush()!=null && getRush().length()>0
               && getRushErrorRate()>getErrorRate();
    }
}
