package com.ban.vehicle_management.infrastructure.ai.extraction;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult.BlockKind;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult.ExtractedBlock;
import java.util.ArrayList;
import java.util.List;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Heading;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.springframework.stereotype.Component;

@Component
public class MarkdownDocumentExtractor implements KnowledgeDocumentExtractor {

    @Override
    public boolean supports(String extension) {
        return "md".equals(extension) || "markdown".equals(extension);
    }

    @Override
    public KnowledgeExtractionResult extract(byte[] bytes, String extension, String mimeType) {
        Node document = Parser.builder().build()
                .parse(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        List<ExtractedBlock> blocks = new ArrayList<>();
        HeadingVisitor visitor = new HeadingVisitor(blocks);
        document.accept(visitor);
        String title = blocks.stream()
                .filter(block -> block.kind() == BlockKind.HEADING)
                .map(ExtractedBlock::text)
                .findFirst()
                .orElse(null);
        return new KnowledgeExtractionResult(title, blocks, 1);
    }

    private static final class HeadingVisitor extends AbstractVisitor {

        private final List<ExtractedBlock> blocks;
        private String headingPath;

        private HeadingVisitor(List<ExtractedBlock> blocks) {
            this.blocks = blocks;
        }

        @Override
        public void visit(Heading heading) {
            String text = inlineText(heading);
            if (!text.isBlank()) {
                headingPath = text.strip();
                blocks.add(new ExtractedBlock(headingPath, BlockKind.HEADING, null, headingPath));
            }
            visitChildren(heading);
        }

        @Override
        public void visit(Paragraph paragraph) {
            String text = inlineText(paragraph);
            if (!text.isBlank()) {
                BlockKind kind = isInsideList(paragraph) ? BlockKind.LIST : BlockKind.PARAGRAPH;
                blocks.add(new ExtractedBlock(text.strip(), kind, null, headingPath));
            }
        }

        @Override
        public void visit(FencedCodeBlock codeBlock) {
            addCode(codeBlock.getLiteral());
        }

        @Override
        public void visit(IndentedCodeBlock codeBlock) {
            addCode(codeBlock.getLiteral());
        }

        @Override
        public void visit(BlockQuote blockQuote) {
            visitChildren(blockQuote);
        }

        private void addCode(String literal) {
            if (literal != null && !literal.isBlank()) {
                blocks.add(new ExtractedBlock(literal.strip(), BlockKind.PARAGRAPH, null, headingPath));
            }
        }

        private boolean isInsideList(Node node) {
            Node parent = node.getParent();
            while (parent != null) {
                if (parent instanceof ListItem || parent instanceof BulletList || parent instanceof OrderedList) {
                    return true;
                }
                parent = parent.getParent();
            }
            return false;
        }

        private String inlineText(Node node) {
            StringBuilder builder = new StringBuilder();
            collectText(node, builder);
            return builder.toString().replaceAll("\\s+", " ").strip();
        }

        private void collectText(Node node, StringBuilder builder) {
            Node child = node.getFirstChild();
            while (child != null) {
                if (child instanceof Text text) {
                    builder.append(text.getLiteral());
                } else if (child instanceof Code code) {
                    builder.append(code.getLiteral());
                } else if (child instanceof Heading || child instanceof Paragraph) {
                    // nested block handled by its own visitor
                } else {
                    collectText(child, builder);
                }
                child = child.getNext();
            }
        }
    }
}