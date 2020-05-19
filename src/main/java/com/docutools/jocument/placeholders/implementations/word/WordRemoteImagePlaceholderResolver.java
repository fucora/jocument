package com.docutools.jocument.placeholders.implementations.word;

import com.docutools.jocument.PlaceholderResolver;
import com.docutools.jocument.PostProcessingResolver;
import com.docutools.jocument.impl.word.WordImageUtils;
import com.docutools.jocument.impl.word.WordUtilities;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class WordRemoteImagePlaceholderResolver implements PostProcessingResolver<XWPFDocument> {
    private static final String URL_PATTERN = "(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    private static final Pattern COMPILED_URL_PATTERN = Pattern.compile(URL_PATTERN);
    private final List<String> acceptedDomains;

    public WordRemoteImagePlaceholderResolver(List<String> acceptedDomains) {
        this.acceptedDomains = acceptedDomains;
    }


    @Override
    public void documentGenerationFinished(XWPFDocument document, PlaceholderResolver resolver) {
        WordUtilities.getAllParagraphsMatchingPlaceholder("remoteImage %s".formatted(URL_PATTERN), document)
                .forEach(xwpfParagraph -> {
                    var stringParagraph = WordUtilities.toString(xwpfParagraph);
                    var runs = xwpfParagraph.getRuns();
                    //Delete all the runs
                    for (int i = runs.size() - 1; i >= 0; i--) {
                        xwpfParagraph.removeRun(0);
                    }
                    COMPILED_URL_PATTERN.matcher(stringParagraph)
                            .results()
                            .map(MatchResult::group)
                            .filter(url -> acceptedDomains.stream().anyMatch(url::startsWith) && URLConnection.guessContentTypeFromName(url).startsWith("image"))
                            .map(URI::create)
                            .forEach(uri -> {
                                try {
                                    var tempFile = File.createTempFile("jocument-remote-img", "");
                                    var channel = Channels.newChannel(uri.toURL().openStream());
                                    var fileOutputStream = new FileOutputStream(tempFile);
                                    fileOutputStream.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
                                    fileOutputStream.close();
                                    channel.close();

                                    WordImageUtils.insertImage(xwpfParagraph, tempFile.toPath());
                                    tempFile.delete();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                });
    }
}
