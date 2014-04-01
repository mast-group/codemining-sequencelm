/**
 * 
 */
package codemining.java.codedataextractors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

import codemining.java.codeutils.JavaASTExtractor;
import codemining.java.tokenizers.JavaTokenizer;
import codemining.lm.ngram.NGramLM;

import com.google.common.collect.Sets;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class MethodNGrams extends ASTVisitor {

	private static final Logger LOGGER = Logger.getLogger(MethodNGrams.class
			.getName());

	private static Collection<File> getAllFiles(final String baseDir) {
		final File baseDirectory = new File(baseDir);
		return FileUtils.listFiles(baseDirectory, new RegexFileFilter(
				".*\\.java$"), DirectoryFileFilter.DIRECTORY);
	}

	public static void main(final String args[]) throws IOException {
		final NGramLM n = new NGramLM(3, new JavaTokenizer()); // TODO

		for (final File f : getAllFiles(args[0])) {
			final MethodNGrams m = new MethodNGrams();
			final JavaASTExtractor astExtractor = new JavaASTExtractor(
					false);
			final CompilationUnit cu = astExtractor.getAST(f);
			cu.accept(m);
			n.addSentences(m.extractedSentences, true);
		}
		System.out.println(n);
	}

	public Set<List<String>> extractedSentences = Sets.newHashSet();

	private final ArrayDeque<List<String>> nGramStack = new ArrayDeque<List<String>>();

	@Override
	public void endVisit(MethodDeclaration node) {
		nGramStack.peek().add("END");
		extractedSentences.add(nGramStack.pop());
		super.endVisit(node);
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		nGramStack.push(new ArrayList<String>());
		nGramStack.peek().add("START");
		return super.visit(node);
	}

	@Override
	public boolean visit(MethodInvocation node) {
		if (nGramStack.size() != 0) {
			nGramStack.peek().add(node.getName().toString());
		}
		return super.visit(node);
	}

}
