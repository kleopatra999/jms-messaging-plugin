package com.redhat.jenkins.plugins.ci;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;

import java.io.IOException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.redhat.jenkins.plugins.ci.messaging.JMSMessagingProvider;
import com.redhat.jenkins.plugins.ci.messaging.MessagingProviderOverrides;
import com.redhat.utils.MessageUtils;
import com.redhat.utils.MessageUtils.MESSAGE_TYPE;

/*
 * The MIT License
 *
 * Copyright (c) Red Hat, Inc.
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
public class CIMessageBuilder extends Builder {

    private String providerName;
    private MessagingProviderOverrides overrides;
    private MESSAGE_TYPE messageType;
    private String messageProperties;
    private String messageContent;
    private boolean failOnError = false;


    public String getProviderName() {
        return providerName;
    }
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
    public MessagingProviderOverrides getOverrides() {
        return overrides;
    }
    public void setOverrides(MessagingProviderOverrides overrides) {
        this.overrides = overrides;
    }
    public MESSAGE_TYPE getMessageType() {
        return messageType;
    }
    public void setMessageType(MESSAGE_TYPE messageType) {
        this.messageType = messageType;
    }
    public String getMessageProperties() {
        return messageProperties;
    }
    public void setMessageProperties(String messageProperties) {
        this.messageProperties = messageProperties;
    }
    public String getMessageContent() {
        return messageContent;
    }
    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    @DataBoundSetter
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    @DataBoundConstructor
    public CIMessageBuilder(final String providerName,
                            final MessagingProviderOverrides overrides,
                            final MESSAGE_TYPE messageType,
                            final String messageProperties,
                            final String messageContent) {
        super();
        this.providerName = providerName;
        this.overrides = overrides;
        this.messageType = messageType;
        this.messageProperties = messageProperties;
        this.messageContent = messageContent;
    }

    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return MessageUtils.sendMessage(build, listener,
                getProviderName(),
                getOverrides(),
                getMessageType(),
                failOnError, getMessageProperties(),
                getMessageContent());
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        private String providerName;
        private MessagingProviderOverrides overrides;
        private MESSAGE_TYPE messageType;
        private String messageProperties;
        private String messageContent;
        private boolean failOnError;

        public boolean isFailOnError() {
            return failOnError;
        }

        public void setFailOnError(boolean failOnError) {
            this.failOnError = failOnError;
        }

        public DescriptorImpl() {
            load();
        }

        public String getProviderName() {
            return providerName;
        }

        public void setProviderName(String providerName) {
            this.providerName = providerName;
        }

        public MessagingProviderOverrides getOverrides() {
            return overrides;
        }

        public void setOverrides(MessagingProviderOverrides overrides) {
            this.overrides = overrides;
        }

        public MESSAGE_TYPE getMessageType() {
            return messageType;
        }

        public void setMessageType(MESSAGE_TYPE messageType) {
            this.messageType = messageType;
        }

        public String getMessageProperties() {
            return messageProperties;
        }

        public void setMessageProperties(String messageProperties) {
            this.messageProperties = messageProperties;
        }

        public String getMessageContent() {
            return messageContent;
        }

        public void setMessageContent(String messageContent) {
            this.messageContent = messageContent;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> arg0) {
            return true;
        }

        @Override
        public CIMessageBuilder newInstance(StaplerRequest sr, JSONObject jo) {
            MessagingProviderOverrides mpo = null;
            if (!jo.getJSONObject("overrides").isNullObject()) {
                mpo = new MessagingProviderOverrides(jo.getJSONObject("overrides").getString("topic"));
            }
            boolean failOnError = jo.getBoolean("failOnError");
            CIMessageBuilder mb = new CIMessageBuilder(jo.getString("providerName"),
                    mpo,
                    MESSAGE_TYPE.fromString(jo.getString("messageType")),
                    jo.getString("messageProperties"),
                    jo.getString("messageContent"));
            mb.setFailOnError(failOnError);
            return mb;
        }

        @Override
        public boolean configure(StaplerRequest sr, JSONObject formData) throws FormException {
            MessagingProviderOverrides mpo = null;
            if (!formData.getJSONObject("overrides").isNullObject()) {
                mpo = new MessagingProviderOverrides(formData.getJSONObject("overrides").getString("topic"));
            }
            setProviderName(formData.optString("providerName"));
            setOverrides(mpo);
            setMessageType(MESSAGE_TYPE.fromString(formData.optString("messageType")));
            setMessageProperties(formData.optString("messageProperties"));
            setMessageContent(formData.optString("messageContent"));
            boolean failOnError = formData.optBoolean("failOnError");
            setFailOnError(failOnError);

            try {
                CIMessageBuilder mb = new CIMessageBuilder(getProviderName(),
                    getOverrides(),
                    getMessageType(),
                    getMessageProperties(),
                    getMessageContent());
                mb.setFailOnError(failOnError);
            } catch (Exception e) {
                throw new FormException("Failed to initialize notifier - check your global notifier configuration settings", e, "");
            }
            save();
            return super.configure(sr, formData);
        }

        @Override
        public String getDisplayName() {
            return "CI Notifier";
        }

        public ListBoxModel doFillProviderNameItems() {
            ListBoxModel items = new ListBoxModel();
            for (JMSMessagingProvider provider: GlobalCIConfiguration.get().getConfigs()) {
                items.add(provider.getName());
            }
            return items;
        }

        public ListBoxModel doFillMessageTypeItems(@QueryParameter String messageType) {
            MESSAGE_TYPE current = MESSAGE_TYPE.fromString(messageType);
            ListBoxModel items = new ListBoxModel();
            for (MESSAGE_TYPE t : MESSAGE_TYPE.values()) {
                items.add(new ListBoxModel.Option(t.toDisplayName(), t.name(), (t == current) || items.size() == 0));
            }
            return items;
        }
    }
}
