/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cam2.exif;

import android.util.Log;

import com.google.cam2.exif.*;
import com.google.cam2.exif.ExifParser;
import com.google.cam2.exif.ExifTag;
import com.google.cam2.exif.IfdData;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class reads the EXIF header of a JPEG file and stores it in
 * {@link com.google.cam2.exif.ExifData}.
 */
class ExifReader {
    private static final String TAG = "ExifReader";

    private final ExifInterface mInterface;

    ExifReader(ExifInterface iRef) {
        mInterface = iRef;
    }

    /**
     * Parses the inputStream and and returns the EXIF data in an
     * {@link com.google.cam2.exif.ExifData}.
     *
     * @throws ExifInvalidFormatException
     * @throws IOException
     */
    protected com.google.cam2.exif.ExifData read(InputStream inputStream) throws ExifInvalidFormatException,
            IOException {
        com.google.cam2.exif.ExifParser parser = com.google.cam2.exif.ExifParser.parse(inputStream, mInterface);
        com.google.cam2.exif.ExifData exifData = new com.google.cam2.exif.ExifData(parser.getByteOrder());
        com.google.cam2.exif.ExifTag tag = null;

        int event = parser.next();
        while (event != com.google.cam2.exif.ExifParser.EVENT_END) {
            switch (event) {
                case com.google.cam2.exif.ExifParser.EVENT_START_OF_IFD:
                    exifData.addIfdData(new com.google.cam2.exif.IfdData(parser.getCurrentIfd()));
                    break;
                case com.google.cam2.exif.ExifParser.EVENT_NEW_TAG:
                    tag = parser.getTag();
                    if (!tag.hasValue()) {
                        parser.registerForTagValue(tag);
                    } else {
                        exifData.getIfdData(tag.getIfd()).setTag(tag);
                    }
                    break;
                case com.google.cam2.exif.ExifParser.EVENT_VALUE_OF_REGISTERED_TAG:
                    tag = parser.getTag();
                    if (tag.getDataType() == ExifTag.TYPE_UNDEFINED) {
                        parser.readFullTagValue(tag);
                    }
                    exifData.getIfdData(tag.getIfd()).setTag(tag);
                    break;
                case com.google.cam2.exif.ExifParser.EVENT_COMPRESSED_IMAGE:
                    byte buf[] = new byte[parser.getCompressedImageSize()];
                    if (buf.length == parser.read(buf)) {
                        exifData.setCompressedThumbnail(buf);
                    } else {
                        Log.w(TAG, "Failed to read the compressed thumbnail");
                    }
                    break;
                case ExifParser.EVENT_UNCOMPRESSED_STRIP:
                    buf = new byte[parser.getStripSize()];
                    if (buf.length == parser.read(buf)) {
                        exifData.setStripBytes(parser.getStripIndex(), buf);
                    } else {
                        Log.w(TAG, "Failed to read the strip bytes");
                    }
                    break;
            }
            event = parser.next();
        }
        return exifData;
    }
}
