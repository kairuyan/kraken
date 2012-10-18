package org.krakenapps.docxcod;

import static org.krakenapps.docxcod.util.XMLDocHelper.evaluateXPath;
import static org.krakenapps.docxcod.util.XMLDocHelper.evaluateXPathExpr;
import static org.krakenapps.docxcod.util.XMLDocHelper.newDocumentBuilder;
import static org.krakenapps.docxcod.util.XMLDocHelper.newXPath;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.krakenapps.docxcod.util.XMLDocHelper.NodeListWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TableDirectiveParser implements OOXMLProcessor {
	private Logger logger = LoggerFactory.getLogger(getClass().getName());

	static String[] augmentedDirectives = {
			"@before-row",
			"@after-row",
			// "@before-paragraph",
			// "@after-paragraph"
	};

	public void process(OOXMLPackage pkg) {
		extractMergeField(pkg);
		unwrapMagicNode(pkg);
	}

	private void extractMergeField(OOXMLPackage pkg) throws TransformerFactoryConfigurationError {
		InputStream f = null;
		try {
			f = new FileInputStream(new File(pkg.getDataDir(), "word/document.xml"));
			Document doc = newDocumentBuilder().parse(f);

			XPath xpath = newXPath(doc);
			NodeList nodeList = evaluateXPath(xpath,
					"//w:tbl//*[name()='w:fldChar' or name()='w:instrText' or name()='w:fldSimple']", doc);

			XPathExpression xpFldSimpleText = xpath.compile("w:r/w:t");

			List<Directive> directives = DirectiveExtractor.parseNodeList(nodeList);
			for (Directive d : directives) {

				Node n = d.getPosition();

				String directive = d.getDirectiveString();
				logger.debug("{} {}", new Object[] { n.getNodeName(), directive });

				/*
				 * AugmentedDirective: starts with '@', refer
				 * 'augmented-directive' static variable. Their contents will be
				 * moved to proper position through directive.
				 * FreemarkerDirective: others
				 */
				if (directive.charAt(0) == '@') {
					handleAugmentedDirective(doc, n, directive);
				} else {
					handleFreemarkerDirective(xpFldSimpleText, n, directive);
				}

			}

			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer
					.transform(new DOMSource(doc), new StreamResult(new File(pkg.getDataDir(), "word/document.xml")));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			safeClose(f);
		}
	}

	private void unwrapMagicNode(OOXMLPackage pkg) {
		/*
		 * augmented directive is processed with standard XML API. Because
		 * Freemarker directive is not legal XML element, contents of augmented
		 * directive should be wrapped to legal XML element, "KMagicNode", in
		 * extractMergeField method.
		 * 
		 * In this method, all KMagicNode element will be translated into
		 * Freemarker directive without XML API.
		 */
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		FileInputStream fis = null;

		try {
			File docXml = new File(pkg.getDataDir(), "word/document.xml");
			File newDocXml = new File(docXml + ".new");

			fis = new FileInputStream(docXml);
			String xmlString = new Scanner(fis, "UTF-8").useDelimiter("\\A").next();

			fis.close();

			xmlString = xmlString.replaceAll("<KMagicNode><!\\[CDATA\\[", "<");
			xmlString = xmlString.replaceAll("\\]\\]></KMagicNode>", ">");

			InputStream in = new ByteArrayInputStream(xmlString.getBytes("UTF-8"));
			FileOutputStream fos = new FileOutputStream(newDocXml);

			bis = new BufferedInputStream(in);
			bos = new BufferedOutputStream(fos);

			int len = 0;
			byte[] buf = new byte[1024];
			while ((len = bis.read(buf, 0, 1024)) != -1) {
				bos.write(buf, 0, len);
			}

			bis.close();
			bos.close();
			bis = null;
			bos = null;

			logger.trace("unwrapMagicNode: rename {} to {}", newDocXml, docXml);
			boolean deleteResult = docXml.delete();
			if (deleteResult) {
				logger.trace("unwrapMagicNode: deleting old file success: {}", docXml);
				newDocXml.renameTo(docXml);
			} else {
				logger.error("unwrapMagicNode: deleting old file failed: {}", docXml);
			}

		} catch (Exception e) {
			logger.warn("Exception in unwrapMagicNode", e);
		} finally {
			safeClose(fis);
			safeClose(bis);
			safeClose(bos);
		}
	}

	private static Pattern MAGICNODE_PATTERN = Pattern.compile("<KMagicNode><![CDATA[+(.*)+]]></KMagicNode>");

	private static String parseMagicNode(String in) {
		in = replaceUnicodeQuote(in.trim());
		Matcher matcher = MAGICNODE_PATTERN.matcher(in);

		if (matcher.find() && matcher.groupCount() > 0) {
			String f = matcher.group(1);
			if (f == null)
				f = matcher.group(2);
			f = f.replaceAll("\\\\(.)", "$1");
			return f;
		} else
			return null;
	}

	private static String replaceUnicodeQuote(String in) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < in.length(); ++i) {
			int type = Character.getType(in.codePointAt(i));
			switch (type) {
			case Character.FINAL_QUOTE_PUNCTUATION:
			case Character.INITIAL_QUOTE_PUNCTUATION:
				builder.append('"');
				break;
			default:
				builder.append(in.charAt(i));
				break;
			}
		}
		return builder.toString();
	}

	// unused but leave here for reference
	@SuppressWarnings("unused")
	private static String transformMagicNode(String nodeValue) {
		String result = nodeValue.trim();
		if (result.startsWith("<KMagicNode>")) {
			result = parseMagicNode(result);
		}
		return result;
	}

	public static final String UTF8_BOM = "\uFEFF";

	private void handleFreemarkerDirective(XPathExpression xpFldSimpleText, Node n, String directive)
			throws XPathExpressionException {
		if (n.getNodeName().equals("w:fldSimple")) {
			/* // @formatter:off
            <w:fldSimple w:instr="MERGEFIELD &quot;@before-row#list .vars[\&quot;disk-usage-summary\&quot;] as u&quot; \* MERGEFORMAT">
              <w:r w:rsidR="00C47145">
                <w:rPr>
                  <w:noProof />
                </w:rPr>
                <w:t>«@before-row#list .vars["disk-usage-summa»</w:t>
              </w:r>
            </w:fldSimple>
            move all nodes in fldSimple to out of it.
                        
            */ // @formatter:on
			logger.debug("fldSimple found");
			NodeList t = evaluateXPathExpr(xpFldSimpleText, n);

			System.out.println(directive);
			t.item(0).setTextContent(directive);

			// n : w:fldSimple can contain many w:r in its children
			Node parent = n.getParentNode();
			for (Node c : new NodeListWrapper(n.getChildNodes())) {
				if (c.getNodeName() != null)
					parent.insertBefore(c.cloneNode(true), n);
			}
			parent.removeChild(n);
		} else if (n.getNodeName().equals("w:fldChar")) {
			// @formatter:off
			/*
            <w:r>
              <w:fldChar w:fldCharType="begin" />
            </w:r>
            <w:r>
              <w:instrText xml:space="preserve">MERGEFIELD @after-row#/list \* MERGEFORMAT</w:instrText>
            </w:r>
            <w:r>
              <w:fldChar w:fldCharType="separate" />
            </w:r>
            <w:r> <!-- style of this run will be used -->
              <w:rPr>
                <w:noProof />
              </w:rPr>
              <w:t>«@after-row#/list»</w:t>
            </w:r>
            <w:r>
              <w:rPr>
                <w:noProof />
              </w:rPr>
              <w:fldChar w:fldCharType="end" />
            </w:r>
			 */
            // @formatter:on
			Node firstRun = n.getParentNode();
			Node sibling = firstRun.getNextSibling();
			Node lastRun = null;
			Node newRun = null;
			ArrayList<Node> willBeRemoved = new ArrayList<Node>();
			willBeRemoved.add(firstRun);

			while (sibling != null)
			{
				if (sibling.getNodeName().equals("w:r"))
				{
					// all nodes in w:fldChar except 'newRun' will be removed
					// finally.
					willBeRemoved.add(sibling);

					Node fldCharNode = findFldCharNode(sibling);
					if (fldCharNode == null) {
						sibling = sibling.getNextSibling();
						continue;
					}
					NamedNodeMap attributes = fldCharNode.getAttributes();
					Node namedItem = attributes.getNamedItem("w:fldCharType");
					if (namedItem == null) {
						sibling = sibling.getNextSibling();
						continue;
					}
					if (namedItem.getNodeValue().equals("separate")) {
						sibling = sibling.getNextSibling();

						// newRun will not be removed
						newRun = sibling;
						// skip whitespace elements and find first w:r.
						while (!newRun.getNodeName().equals("w:r")) {
							newRun = newRun.getNextSibling();
						}

						// replace contents of first w:r. so formating style of
						// newRun will preserved.
						Node textNode = findTextNode(newRun);
						if (textNode == null) {
							logger.warn("no text-containing run element found with directive. skipped. : {}", directive);
							continue;
						}

						textNode.setTextContent(directive);

						continue;// live
					}
					if (namedItem.getNodeValue().equals("end")) {
						lastRun = sibling;
						break;
					}
				}
				sibling = sibling.getNextSibling();
			}
			willBeRemoved.remove(newRun);

			if (lastRun != null) { // found matching "end" fldChar
				Node parentNode = firstRun.getParentNode();
				for (Node node : willBeRemoved) {
					parentNode.removeChild(node);
				}
			} else {
				logger.warn("no matching \"end\" fldChar found");
			}
		}
	}

	/*
	 * Augmented directive is annotated Freemarker directive. The directive
	 * contains annotation on position of the Freemarker directive, such as
	 * 'before row', 'after row'.
	 * 
	 * This method removes annotated node and inserts the magic node contains
	 * Freemarker directive which is handled by unwrapMagicNode.
	 */
	private void handleAugmentedDirective(Document doc, Node n, String directive) {
		if (n.getNodeName().equals("w:fldSimple"))
		{
			Node targetPara = n.getParentNode().getParentNode();
			Node parentOfPara = targetPara;

			String prefix = findPrefix(directive);

			if (prefix == null) {
				logger.debug("unsupported augmented directive: {}", directive);
				return;
			}

			if (prefix.contains("row"))
				do {
					targetPara = targetPara.getParentNode();
				} while (!targetPara.getNodeName().equals("w:tr"));
			else if (prefix.contains("paragraph"))
			{
				logger.debug("not supported yet");
				return;
			}

			parentOfPara = targetPara.getParentNode();

			if (directive.startsWith("@after-row")) {
				targetPara = targetPara.getNextSibling();
			}

			// insert magic node
			parentOfPara.insertBefore(getMagicNode(doc, unwrapAugmentedDirective(directive)), targetPara);

			// remove annotated node
			n.getParentNode().removeChild(n);
		}
		else if (n.getNodeName().equals("w:fldChar"))
		{
			// insert magic node
			Node targetPara = n;
			Node parentOfPara = targetPara;

			String prefix = findPrefix(directive);

			if (prefix == null) {
				logger.debug("unsupported augmented directive: {}", directive);
				return;
			}

			if (prefix.contains("row"))
				do {
					targetPara = targetPara.getParentNode();
				} while (!targetPara.getNodeName().equals("w:tr"));
			else if (prefix.contains("paragraph"))
			{
				logger.debug("not supported yet");
				return;
			}

			parentOfPara = targetPara.getParentNode();

			if (directive.startsWith("@after-row")) {
				targetPara = targetPara.getNextSibling();
			}

			parentOfPara.insertBefore(getMagicNode(doc, unwrapAugmentedDirective(directive)), targetPara);

			// remove annotated node
			Node firstRun = n.getParentNode();
			Node sibling = firstRun.getNextSibling();
			Node lastRun = null;
			ArrayList<Node> willBeRemoved = new ArrayList<Node>();
			willBeRemoved.add(firstRun);

			while (sibling != null)
			{
				if (sibling.getNodeName().equals("w:r"))
				{
					willBeRemoved.add(sibling);
					Node fldCharNode = findFldCharNode(sibling);
					if (fldCharNode == null) {
						sibling = sibling.getNextSibling();
						continue;
					}
					NamedNodeMap attributes = fldCharNode.getAttributes();
					Node namedItem = attributes.getNamedItem("w:fldCharType");
					if (namedItem == null) {
						sibling = sibling.getNextSibling();
						continue;
					}
					if (namedItem.getNodeValue().equals("end")) {
						lastRun = sibling;
						break;
					}
				}
				sibling = sibling.getNextSibling();
			}
			if (lastRun != null) { // found matching "end" fldChar
				Node parentNode = firstRun.getParentNode();
				for (Node node : willBeRemoved) {
					parentNode.removeChild(node);
				}
			} else {
				logger.warn("no matching \"end\" fldChar found");
			}
		}
		else
		{
			logger.warn("not supported yet: {}", n.getNodeName());
		}
	}

	private Node findFldCharNode(Node sibling) {
		for (Node n : new NodeListWrapper(sibling.getChildNodes())) {
			if (n.getNodeName().equals("w:fldChar")) {
				return n;
			}
		}
		return null;
	}

	private Node findTextNode(Node sibling) {
		for (Node n : new NodeListWrapper(sibling.getChildNodes())) {
			if (n.getNodeName().equals("w:t")) {
				return n;
			}
		}
		return null;
	}

	private String findPrefix(String directive) {
		for (String ad : augmentedDirectives) {
			if (directive.startsWith(ad)) {
				return ad;
			}
		}
		return null;
	}

	private String unwrapAugmentedDirective(String directive) {
		for (String ad : augmentedDirectives) {
			if (directive.startsWith(ad)) {
				return directive.substring(ad.length()).trim();
			}
		}
		return directive;
	}

	private void safeClose(Closeable f) {
		if (f == null)
			return;
		try {
			f.close();
		} catch (Exception e) {
			// ignore
		}
	}

	private Node getMagicNode(Document doc, String content) {
		Element magicNode = doc.createElement("KMagicNode");
		magicNode.appendChild(doc.createCDATASection(content));
		return magicNode;
	}
}
