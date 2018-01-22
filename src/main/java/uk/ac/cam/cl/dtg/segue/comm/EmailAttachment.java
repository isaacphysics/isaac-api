/*
 * Copyright 2017 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.comm;

/**
 * POJO for representing an email attachment.
 */
public class EmailAttachment {

    private String fileName;
    private String mimeType;
    private Object attachment;

    public EmailAttachment(String fileName, String mimeType, Object attachment) {
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.attachment = attachment;
    }

    public String getFileName() {
        return fileName;
    }


    public String getMimeType() {
        return mimeType;
    }

    public Object getAttachment() {
        return attachment;
    }

}
