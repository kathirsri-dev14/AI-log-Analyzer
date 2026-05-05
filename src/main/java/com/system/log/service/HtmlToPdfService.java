package com.system.log.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HtmlToPdfService {

	public byte[] convertHtmlToPdf(String html) {

	    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {

	        // 🔥 FIX ONLY CODE BLOCKS
	        html = fixPreTags(html);

	        PdfRendererBuilder builder = new PdfRendererBuilder();
	        builder.withHtmlContent(html, null);
	        builder.toStream(os);
	        builder.run();

	        return os.toByteArray();

	    } catch (Exception e) {
	        e.printStackTrace();
	        throw new RuntimeException("PDF generation failed");
	    }
	}

	private String fixPreTags(String html) {

		Pattern pattern = Pattern.compile("<pre>(.*?)</pre>", Pattern.DOTALL);
		Matcher matcher = pattern.matcher(html);

		StringBuffer result = new StringBuffer();

		while (matcher.find()) {

			String code = matcher.group(1);

			String escapedCode = code.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

			matcher.appendReplacement(result, "<pre>" + escapedCode + "</pre>");
		}

		matcher.appendTail(result);

		return result.toString();
	}
	}
