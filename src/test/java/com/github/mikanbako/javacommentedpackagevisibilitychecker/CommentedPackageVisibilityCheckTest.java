package com.github.mikanbako.javacommentedpackagevisibilitychecker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Locale;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.DefaultLogger;
import com.puppycrawl.tools.checkstyle.ModuleFactory;
import com.puppycrawl.tools.checkstyle.TreeWalker;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;

public class CommentedPackageVisibilityCheckTest
{
    private static class TestLogger extends DefaultLogger
    {
        public TestLogger(OutputStream aOS, boolean aCloseStreamsAfterUse)
        {
            super(aOS, aCloseStreamsAfterUse);
        }

        @Override
        public void auditStarted(AuditEvent aEvt)
        {
            // no operation.
        }

        @Override
        public void fileFinished(AuditEvent aEvt)
        {
            // no operation.
        }

        @Override
        public void fileStarted(AuditEvent aEvt)
        {
            // no operation.
        }
    }

    private static final class TargetModuleFactory implements ModuleFactory
    {
        public Object createModule(String aName) throws CheckstyleException
        {
            if (aName.equals(CommentedPackageVisibilityCheck.class.getName())) {
                return new CommentedPackageVisibilityCheck();
            } else if (aName.equals(TreeWalker.class.getName())) {
                TreeWalker treeWaler = new TreeWalker();
                treeWaler.setModuleFactory(this);

                return treeWaler;
            }

            throw new CheckstyleException(String.format("%s is not found.", aName));
        }
    }

    private static final String PROPERTY_TEST_INPUTS_DIRECTORY = "testinputs.dir";

    private ByteArrayOutputStream mByteArrayOutputStream;

    private PrintStream mStream;

    @Before
    public void setUp()
    {
        mByteArrayOutputStream = new ByteArrayOutputStream();
        mStream = new PrintStream(mByteArrayOutputStream);
    }

    private String getPath(String aFileName) throws IOException
    {
        String testInputsDirectory = System.getProperty(PROPERTY_TEST_INPUTS_DIRECTORY);
        if (testInputsDirectory == null) {
            throw new IllegalStateException(String.format("Set system property '%s'.", PROPERTY_TEST_INPUTS_DIRECTORY));
        }

        File file = new File(testInputsDirectory, aFileName);

        return file.getCanonicalPath();
    }

    private void verify(Configuration aConfiguration, String aFilePath, String[] aExpected)
            throws CheckstyleException, IOException
    {
        DefaultConfiguration parentContifugation = new DefaultConfiguration("configuration");

        DefaultConfiguration treeWalkerConfiguration = new DefaultConfiguration(TreeWalker.class.getName());
        treeWalkerConfiguration.addChild(aConfiguration);
        parentContifugation.addChild(treeWalkerConfiguration);

        Checker checker = new Checker();
        checker.setLocaleCountry("");
        checker.setLocaleLanguage("");
        checker.addListener(new TestLogger(mStream, true));
        checker.setModuleFactory(new TargetModuleFactory());
        checker.configure(parentContifugation);

        int errorCount = checker.process(Collections.singletonList(new File(aFilePath)));

        ByteArrayInputStream inputStream = new ByteArrayInputStream(mByteArrayOutputStream.toByteArray());
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(inputStream));

        for (int i = 0; i < aExpected.length; i++) {
            String exceptedMessage = String.format(Locale.ENGLISH, "%s:%s", aFilePath, aExpected[i]);
            String actualMessage = reader.readLine();

            Assert.assertEquals(exceptedMessage, actualMessage);
        }

        Assert.assertEquals("Unexpected error : " + reader.readLine(),
                aExpected.length, errorCount);

        checker.destroy();
    }

    private void verify(Configuration aConfiguration, String[] aExpected) throws
            CheckstyleException, IOException {
        verify(aConfiguration, getPath("CommentedPackageVisibilityCheckTestInput.java"), aExpected);
    }

    /**
     * Test when setting is default.
     *
     * Format is "&#x2f;&#x2a; package &#x2f;&#x2a;" and letter white space is
     * required.
     */
    @Test
    public void testWithDefaultSetting() throws Exception
    {
        final DefaultConfiguration checkConfig = new DefaultConfiguration(
                CommentedPackageVisibilityCheck.class.getName());
        final String[] expected = {
                "3: 'CommentedPackageVisibilityCheckTestInput' should be commented for package visibility.",
                "5: 'NoCommentedPackageVisibilityClass' should be commented for package visibility.",
                "15: Is visibility of 'InvalidCommentedPackageVisibilityClass' package?",
                "20: 'NoCommentedPackageVisibilityInterface' should be commented for package visibility.",
                "30: Is visibility of 'InvalidCommentedPackageVisibilityInterface' package?",
                "35: 'NoCommentedPackageVisibilityEnum' should be commented for package visibility.",
                "45: Is visibility of 'InvalidCommentedPackageVisibilityEnum' package?",
                "50: 'noCommentedPackageVisibilityField' should be commented for package visibility.",
                "54: Is visibility of 'invalidCommentedPackageVisibilityField' package?",
                "57: 'CommentedPackageVisibilityCheckTestInput' should be commented for package visibility.",
                "69: Is visibility of 'CommentedPackageVisibilityCheckTestInput' package?",
                "75: 'noCommentedPackageVisibilityMethod' should be commented for package visibility.",
                "85: Comment of 'commentdPackageVisibilityWithoutWhitespaceMethod' for package visibility should be add letter whitespace.",
                "91: 'commentedPackageVisibilityWithOtherFormatMethod' should be commented for package visibility.",
                "96: Is visibility of 'invalidCommentedPackageVisibilityMethod' package?",
                "101: Is visibility of 'invalidCommentedPackageVisibilityWithoutSpaceMethod' package?",
        };

        verify(checkConfig, expected);
    }

    /**
     * Test when letter white space is not required.
     *
     * Format is "&#x2f;&#x2a; package &#x2f;&#x2a;".
     */
    @Test
    public void testWithoutRequiringLetterWhiteSpace() throws Exception {
        final DefaultConfiguration checkConfig = new DefaultConfiguration(
                CommentedPackageVisibilityCheck.class.getName());
        checkConfig.addAttribute("requireLatterWhiteSpace", Boolean.FALSE.toString());

        final String[] expected = {
                "3: 'CommentedPackageVisibilityCheckTestInput' should be commented for package visibility.",
                "5: 'NoCommentedPackageVisibilityClass' should be commented for package visibility.",
                "15: Is visibility of 'InvalidCommentedPackageVisibilityClass' package?",
                "20: 'NoCommentedPackageVisibilityInterface' should be commented for package visibility.",
                "30: Is visibility of 'InvalidCommentedPackageVisibilityInterface' package?",
                "35: 'NoCommentedPackageVisibilityEnum' should be commented for package visibility.",
                "45: Is visibility of 'InvalidCommentedPackageVisibilityEnum' package?",
                "50: 'noCommentedPackageVisibilityField' should be commented for package visibility.",
                "54: Is visibility of 'invalidCommentedPackageVisibilityField' package?",
                "57: 'CommentedPackageVisibilityCheckTestInput' should be commented for package visibility.",
                "69: Is visibility of 'CommentedPackageVisibilityCheckTestInput' package?",
                "75: 'noCommentedPackageVisibilityMethod' should be commented for package visibility.",
                "91: 'commentedPackageVisibilityWithOtherFormatMethod' should be commented for package visibility.",
                "96: Is visibility of 'invalidCommentedPackageVisibilityMethod' package?",
                "101: Is visibility of 'invalidCommentedPackageVisibilityWithoutSpaceMethod' package?",
        };

        verify(checkConfig, expected);
    }

    @Test
    public void testWithOneLineCommentFormat() throws Exception {
        final DefaultConfiguration checkConfig = new DefaultConfiguration(
                CommentedPackageVisibilityCheck.class.getName());
        checkConfig.addAttribute("format", "// package\n");

        final String[] expected = {
                "3: 'CommentedPackageVisibilityCheckTestInput' should be commented for package visibility.",
                "5: 'NoCommentedPackageVisibilityClass' should be commented for package visibility.",
                "10: 'CommentedPackageVisibilityClass' should be commented for package visibility.",
                "20: 'NoCommentedPackageVisibilityInterface' should be commented for package visibility.",
                "25: 'CommentedPackageVisibilityInterface' should be commented for package visibility.",
                "35: 'NoCommentedPackageVisibilityEnum' should be commented for package visibility.",
                "40: 'CommentedPackageVisibilityEnum' should be commented for package visibility.",
                "50: 'noCommentedPackageVisibilityField' should be commented for package visibility.",
                "52: 'commentedPackageVisibilityField' should be commented for package visibility.",
                "57: 'CommentedPackageVisibilityCheckTestInput' should be commented for package visibility.",
                "63: 'CommentedPackageVisibilityCheckTestInput' should be commented for package visibility.",
                "75: 'noCommentedPackageVisibilityMethod' should be commented for package visibility.",
                "80: 'commentedPackageVisibilityMethod' should be commented for package visibility.",
                "85: 'commentdPackageVisibilityWithoutWhitespaceMethod' should be commented for package visibility.",
                "107: Is visibility of 'invalidCommentedPackageVisibilityWithOtherFormatMethod' package?",
        };

        verify(checkConfig, expected);
    }
}
