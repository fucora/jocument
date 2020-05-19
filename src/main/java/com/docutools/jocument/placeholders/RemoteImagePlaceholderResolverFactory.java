package com.docutools.jocument.placeholders;

import com.docutools.jocument.PostProcessingResolver;
import com.docutools.jocument.placeholders.implementations.word.WordRemoteImagePlaceholderResolver;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.util.List;

public class RemoteImagePlaceholderResolverFactory {
    public static <T> PostProcessingResolver<T> createRemoteImagePlaceholderResolver(Class<T> documentType, List<String> trustedDomains) {
        if (documentType == XWPFDocument.class) {
            return (PostProcessingResolver<T>) new WordRemoteImagePlaceholderResolver(trustedDomains);
        } else {
            throw new UnsupportedOperationException("The resolver for remote image placeholders is only implemented for Word");
        }
    }
}