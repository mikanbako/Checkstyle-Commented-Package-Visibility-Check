package com.github.mikanbako.javacommentedpackagevisibilitychecker;

import java.util.regex.Pattern;

import org.apache.commons.beanutils.ConversionException;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.LineColumn;
import com.puppycrawl.tools.checkstyle.api.Scope;
import com.puppycrawl.tools.checkstyle.api.ScopeUtils;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.AbstractFormatCheck;

/**
 * <p>Check that package visibility is commented instead of empty modifier.</p>
 *
 * <p>Example :</p>
 * <pre>
 *   &#x2f;&#x2a; package &#x2a;&#x2f; class PackageVisibilityClass {
 *      ...
 *   }
 * </pre>
 *
 * @author Keita Kita
 */
public final class CommentedPackageVisibilityCheck extends AbstractFormatCheck
{
    /**
     * Default format.
     */
    private static final String DEFAULT_FORMAT = "/\\* package \\*/";

    /**
     * Whether latter white space is required for package visibility comment.
     */
    private boolean mRequireLatterWhiteSpace = true;

    public CommentedPackageVisibilityCheck()
            throws ConversionException {
        super(DEFAULT_FORMAT);
    }

    @Override
    public int[] getDefaultTokens()
    {
        return new int[] {
                TokenTypes.CLASS_DEF,
                TokenTypes.ENUM_DEF,
                TokenTypes.INTERFACE_DEF,
                TokenTypes.CTOR_DEF,
                TokenTypes.VARIABLE_DEF,
                TokenTypes.METHOD_DEF,
        };
    }

    @Override
    public void visitToken(DetailAST aAST)
    {
        if (ScopeUtils.isLocalVariableDef(aAST)) {
            return;
        }

        DetailAST modifierAST = aAST.findFirstToken(TokenTypes.MODIFIERS);
        Scope scope = ScopeUtils.getScopeFromMods(modifierAST);

        if (scope == Scope.PACKAGE) {
            checkPackageVisibilityCommentExists(aAST);
        } else {
            checkInappropiratePackageVisibilityComment(aAST);
        }
    }


    public void setRequireLatterWhiteSpace(boolean aRequired) {
        mRequireLatterWhiteSpace = aRequired;
    }

    /**
     * Check whether comment representing package visibility exists.
     *
     * @param aPackageScopeDefinitionAST AST of definition.
     */
    private void checkPackageVisibilityCommentExists(DetailAST aPackageScopeDefinitionAST)
    {
        LineColumn startSearchingPosition = getStartSearchingPosition(aPackageScopeDefinitionAST);
        LineColumn endSearchingPosition = getEndSearchingPosition(aPackageScopeDefinitionAST);

        Pattern commentPattern;
        if (mRequireLatterWhiteSpace) {
            commentPattern = Pattern.compile(String.format("(?:%s)\\s+", getFormat()), getRegexp().flags());
        } else {
            commentPattern = getRegexp();
        }

        if (!existsPackageVisibilityComment(commentPattern, startSearchingPosition, endSearchingPosition)) {
            String ident = aPackageScopeDefinitionAST.findFirstToken(TokenTypes.IDENT).getText();

            if (mRequireLatterWhiteSpace && existsPackageVisibilityComment(getRegexp(), startSearchingPosition, endSearchingPosition)) {
                log(aPackageScopeDefinitionAST.getLineNo(), "packageVisibilityComment.noLetterWhiteSpace", ident);
            } else {
                log(aPackageScopeDefinitionAST.getLineNo(), "noPackageVisibilityComment", ident);
            }
        }
    }


    private void checkInappropiratePackageVisibilityComment(DetailAST aNonPackageScopeDefinitionAST)
    {
        LineColumn startSearchingPosition = getStartSearchingPosition(aNonPackageScopeDefinitionAST);
        LineColumn endSearchingPosition = getEndSearchingPosition(aNonPackageScopeDefinitionAST);

        if (existsPackageVisibilityComment(getRegexp(), startSearchingPosition, endSearchingPosition)) {
            String ident = aNonPackageScopeDefinitionAST.findFirstToken(TokenTypes.IDENT).getText();

            log(aNonPackageScopeDefinitionAST.getLineNo(), "packageVisibilityComment.modifierExists", ident);
        }
    }


    /**
     * Get position to start searching.
     *
     * This position is one of the below.
     *
     * <ul>
     *  <li>A left curly brace just before the definition.</li>
     *  <li>An end of definition just before the definition.</li>
     *  <li>The head of the file.</li>
     * </ul>
     *
     * @param aDefinitionAST AST of definition.
     * @return Position to start searching.
     */
    private static LineColumn getStartSearchingPosition(DetailAST aDefinitionAST)
    {
        DetailAST previousSiblingAST = aDefinitionAST.getPreviousSibling();

        if (previousSiblingAST == null) {
            DetailAST parentAST = aDefinitionAST.getParent();
            if (parentAST == null) {
                return new LineColumn(1, 0);
            }

            // OBJBLOCK is expected.
            return new LineColumn(parentAST.getLineNo(), parentAST.getColumnNo());
        }

        DetailAST currentAST = previousSiblingAST;
        while (true) {
            DetailAST lastChildAST = currentAST.getLastChild();
            if (lastChildAST == null) {
                break;
            }

            currentAST = lastChildAST;
        }

        return new LineColumn(currentAST.getLineNo(), currentAST.getColumnNo());
    }

    /**
     * Get position to end searching.
     *
     * This position is at modifier of the definition.
     *
     * @param aDefinitionAST AST of definition.
     * @return Position to end searching.
     */
    private static LineColumn getEndSearchingPosition(DetailAST aDefinitionAST)
    {
        DetailAST modifierAST = aDefinitionAST.findFirstToken(TokenTypes.IDENT);

        return new LineColumn(modifierAST.getLineNo(), modifierAST.getColumnNo());
    }

    private String getTargetString(LineColumn aStart, LineColumn aEnd)
    {
        StringBuilder targetStringBuilder = new StringBuilder();
        String[] target = getLines();

        for (int lineNumber = aStart.getLine(); lineNumber <= aEnd.getLine(); lineNumber++) {
            int lineIndex = lineNumber - 1;

            targetStringBuilder.append(target[lineIndex]);
            if (lineNumber < aEnd.getLine()) {
                targetStringBuilder.append('\n');
            }
        }

        int endDeletingCount = (target[aEnd.getLine() - 1].length() - 1) - aEnd.getColumn();
        targetStringBuilder.delete(targetStringBuilder.length() - endDeletingCount, targetStringBuilder.length()).
            delete(0, aStart.getColumn());

        return targetStringBuilder.toString();
    }

    private boolean existsPackageVisibilityComment(Pattern aPattern, LineColumn aStart, LineColumn aEnd)
    {
        String target = getTargetString(aStart, aEnd);

        return aPattern.matcher(target).find();
    }
}
