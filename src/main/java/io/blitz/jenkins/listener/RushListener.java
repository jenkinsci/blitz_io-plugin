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
package io.blitz.jenkins.listener;

import io.blitz.curl.rush.IRushListener; 
import io.blitz.curl.rush.RushResult;

/**
 * @author jeffli
 */
public class RushListener implements IRushListener {

    private RushResult rushResult;

    /**
     * Constructor
     */
    public RushListener() {
    }

    /**
     * Called when a successful jobStatus response is received by the client.
     * Always returns true.
     * @param result successful rush result
     * @return true
     */
    @Override
    public boolean onStatus(RushResult result) {
        return true;
    }

    /**
     * Called when a successful jobStatus returns with a status equals
     * 'completed'.
     * @param result successful rush result
     */
    @Override
    public void onComplete(RushResult result) {
        rushResult = result;
    }

    public RushResult getRushResult() {
        return rushResult;
    }
}
