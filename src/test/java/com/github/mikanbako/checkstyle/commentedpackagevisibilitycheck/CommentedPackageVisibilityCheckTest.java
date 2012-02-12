/* Commented package visibility check (Checkstyle plugin)
    Copyright (C) 2012  Keita Kita

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.github.mikanbako.checkstyle.commentedpackagevisibilitycheck;

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

/**
 * Test class for {@link CommentedPackageVisibilityCheck}.
 *
 * @author Keita Kita
 */
public class CommentedPackageVisibilityCheckTest
{
    /**
     * Logger for testing.
     */
    private static class TestLogger extends DefaultLogger
    {
        /**
         * Constructor.
         *
         * @param aOS {@inheritDoc}
         * @param aCloseStreamsAfterUse {@inheritDoc}
         */
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

    /**
     * Factory to create testing check.
     */
    private static final class TargetModuleFactory implements ModuleFactory
    {
        /**
         * {@inheritDoc}
         */
        public Object createModule(String aName) throws CheckstyleException
        {
            if (aName.equals(CommentedPackageVisibilityCheck.class.getName())) {
                return new CommentedPackageVisibilityCheck();
            }
            else if (aName.equals(TreeWalker.class.getName())) {
                final TreeWalker treeWaler = new TreeWalker();
                treeWaler.setModuleFactory(this);

                return treeWaler;
            }

            throw new CheckstyleException(
                    String.format("%s is not found.", aName));
        }
    }

    /**
     * Property name of test inputs directory.
     */
    private static final String PROPERTY_TEST_INPUTS_DIRECTORY =
            "testinputs.dir";

    /**
     * OutputStream to store messages.
     */
    private ByteArrayOutputStream mByteArrayOutputStream;

    /**
     * PrintStream to output messages.
     */
    private PrintStream mStream;

    /**
     * {@inheritDoc}
     */
    @Before
    public void setUp()
    {
        mByteArrayOutputStream = new ByteArrayOutputStream();
        mStream = new PrintStream(mByteArrayOutputStream);
    }

    /**
     * Get path of test input file.
     *
     * @param aFileName Name of file.
     * @return Path of test input file.
     * @throws IOException If I/O error occurs.
     */
    private String getPath(String aFileName) throws IOException
    {
        final String testInputsDirectory =
                System.getProperty(PROPERTY_TEST_INPUTS_DIRECTORY);
        if (testInputsDirectory == null) {
            throw new IllegalStateException(
                    String.format("Set system property '%s'.",
                            PROPERTY_TEST_INPUTS_DIRECTORY));
        }

        final File file = new File(testInputsDirectory, aFileName);

        return file.getCanonicalPath();
    }

    /**
     * Verify specified file using specified configuration.
     *
     * @param aConfiguration Configuration for verification.
     * @param aFilePath File for verification.
     * @param aExpected Expected messages.
     * @throws CheckstyleException If error within Checkstyle occurs.
     * @throws IOException If I/O error occurs.
     */
    private void verify(Configuration aConfiguration, String aFilePath,
            String[] aExpected)
        throws CheckstyleException, IOException
    {
        final DefaultConfiguration parentContifugation =
                new DefaultConfiguration("configuration");

        final DefaultConfiguration treeWalkerConfiguration =
                new DefaultConfiguration(TreeWalker.class.getName());
        treeWalkerConfiguration.addChild(aConfiguration);
        parentContifugation.addChild(treeWalkerConfiguration);

        final Checker checker = new Checker();
        checker.setLocaleCountry("");
        checker.setLocaleLanguage("");
        checker.addListener(new TestLogger(mStream, true));
        checker.setModuleFactory(new TargetModuleFactory());
        checker.configure(parentContifugation);

        final int errorCount = checker.process(
                Collections.singletonList(new File(aFilePath)));

        final ByteArrayInputStream inputStream =
                new ByteArrayInputStream(mByteArrayOutputStream.toByteArray());
        final LineNumberReader reader =
                new LineNumberReader(new InputStreamReader(inputStream));

        for (int i = 0; i < aExpected.length; i++) {
            final String exceptedMessage = String.format(
                    Locale.ENGLISH, "%s:%s", aFilePath, aExpected[i]);
            final String actualMessage = reader.readLine();

            Assert.assertEquals(exceptedMessage, actualMessage);
        }

        Assert.assertEquals("Unexpected error : " + reader.readLine(),
                aExpected.length, errorCount);

        checker.destroy();
    }

    /**
     * Create configuration for this test.
     *
     * @return Created configuration.
     */
    private static DefaultConfiguration createConfiguration()
    {
        return new DefaultConfiguration(
                CommentedPackageVisibilityCheck.class.getName());
    }

    /**
     * Test when setting is default.
     *
     * Format is "&#x2f;&#x2a; package &#x2f;&#x2a;" and letter white space is
     * required.
     *
     * @throws Exception If exception occurs.
     */
    @Test
    public void testWithDefaultSetting() throws Exception
    {
        final DefaultConfiguration checkConfig = createConfiguration();
        final String[] expected = {
            "3: 'CommentedPackageVisibilityCheckTestInput' "
                    + "should be commented for package visibility.",
            "5: 'NoCommentedPackageVisibilityClass' "
                    + "should be commented for package visibility.",
            "15: Is visibility of "
                    + "'InvalidCommentedPackageVisibilityClass' package?",
            "20: 'NoCommentedPackageVisibilityInterface' "
                    + "should be commented for package visibility.",
            "30: Is visibility of "
                    + "'InvalidCommentedPackageVisibilityInterface' package?",
            "35: 'NoCommentedPackageVisibilityEnum' "
                    + "should be commented for package visibility.",
            "45: Is visibility of "
                    + "'InvalidCommentedPackageVisibilityEnum' package?",
            "50: 'noCommentedPackageVisibilityField' "
                    + "should be commented for package visibility.",
            "54: Is visibility of "
                    + "'invalidCommentedPackageVisibilityField' package?",
            "57: 'CommentedPackageVisibilityCheckTestInput' "
                    + "should be commented for package visibility.",
            "69: Is visibility of "
                    + "'CommentedPackageVisibilityCheckTestInput' package?",
            "75: 'noCommentedPackageVisibilityMethod' "
                    + "should be commented for package visibility.",
            "85: Comment of "
                    + "'commentdPackageVisibilityWithoutWhitespaceMethod' "
                    + "for package visibility should be add letter whitespace.",
            "91: 'commentedPackageVisibilityWithOtherFormatMethod' "
                    + "should be commented for package visibility.",
            "96: Is visibility of "
                    + "'invalidCommentedPackageVisibilityMethod' package?",
            "101: Is visibility of "
                    + "'invalidCommentedPackageVisibilityWithoutSpaceMethod' "
                    + "package?",
        };

        verify(checkConfig,
                getPath("CommentedPackageVisibilityCheckTestInput.java"),
                expected);
    }

    /**
     * Test when letter white space is not required.
     *
     * Format is "&#x2f;&#x2a; package &#x2f;&#x2a;".
     *
     * @throws Exception If Exception occurs.
     */
    @Test
    public void testWithoutRequiringLetterWhiteSpace() throws Exception
    {
        final DefaultConfiguration checkConfig = createConfiguration();
        checkConfig.addAttribute(
                "requireLatterWhiteSpace", Boolean.FALSE.toString());

        final String[] expected = {
            "3: 'CommentedPackageVisibilityCheckTestInput' "
                    + "should be commented for package visibility.",
            "5: 'NoCommentedPackageVisibilityClass' "
                    + "should be commented for package visibility.",
            "15: Is visibility of "
                    + "'InvalidCommentedPackageVisibilityClass' package?",
            "20: 'NoCommentedPackageVisibilityInterface' "
                    + "should be commented for package visibility.",
            "30: Is visibility of "
                    + "'InvalidCommentedPackageVisibilityInterface' package?",
            "35: 'NoCommentedPackageVisibilityEnum' "
                    + "should be commented for package visibility.",
            "45: Is visibility of "
                    + "'InvalidCommentedPackageVisibilityEnum' package?",
            "50: 'noCommentedPackageVisibilityField' "
                    + "should be commented for package visibility.",
            "54: Is visibility of "
                    + "'invalidCommentedPackageVisibilityField' package?",
            "57: 'CommentedPackageVisibilityCheckTestInput' "
                    + "should be commented for package visibility.",
            "69: Is visibility of "
                    + "'CommentedPackageVisibilityCheckTestInput' package?",
            "75: 'noCommentedPackageVisibilityMethod' "
                    + "should be commented for package visibility.",
            "91: 'commentedPackageVisibilityWithOtherFormatMethod' "
                    + "should be commented for package visibility.",
            "96: Is visibility of "
                    + "'invalidCommentedPackageVisibilityMethod' package?",
            "101: Is visibility of "
                    + "'invalidCommentedPackageVisibilityWithoutSpaceMethod' "
                    + "package?",
        };

        verify(checkConfig,
                getPath("CommentedPackageVisibilityCheckTestInput.java"),
                expected);
    }

    /**
     * Test when format is one line comment.
     *
     * Format is "// package".
     *
     * @throws Exception If Exception occurs.
     */
    @Test
    public void testWithOneLineCommentFormat() throws Exception
    {
        final DefaultConfiguration checkConfig = createConfiguration();
        checkConfig.addAttribute("format", "// package\n");

        final String[] expected = {
            "3: 'CommentedPackageVisibilityCheckTestInput' "
                    + "should be commented for package visibility.",
            "5: 'NoCommentedPackageVisibilityClass' "
                    + "should be commented for package visibility.",
            "10: 'CommentedPackageVisibilityClass' "
                    + "should be commented for package visibility.",
            "20: 'NoCommentedPackageVisibilityInterface' "
                    + "should be commented for package visibility.",
            "25: 'CommentedPackageVisibilityInterface' "
                    + "should be commented for package visibility.",
            "35: 'NoCommentedPackageVisibilityEnum' "
                    + "should be commented for package visibility.",
            "40: 'CommentedPackageVisibilityEnum' "
                    + "should be commented for package visibility.",
            "50: 'noCommentedPackageVisibilityField' "
                    + "should be commented for package visibility.",
            "52: 'commentedPackageVisibilityField' "
                    + "should be commented for package visibility.",
            "57: 'CommentedPackageVisibilityCheckTestInput' "
                    + "should be commented for package visibility.",
            "63: 'CommentedPackageVisibilityCheckTestInput' "
                    + "should be commented for package visibility.",
            "75: 'noCommentedPackageVisibilityMethod' "
                    + "should be commented for package visibility.",
            "80: 'commentedPackageVisibilityMethod' "
                    + "should be commented for package visibility.",
            "85: 'commentdPackageVisibilityWithoutWhitespaceMethod' "
                    + "should be commented for package visibility.",
            "107: Is visibility of "
                    + "'invalidCommentedPackageVisibility"
                    + "WithOtherFormatMethod' package?",
        };

        verify(checkConfig,
                getPath("CommentedPackageVisibilityCheckTestInput.java"),
                expected);
    }

    /**
     * Test when the checker checks source in default package.
     *
     * @throws Exception If Exception occurs.
     */
    @Test
    public void testWithDefaultPackageSource() throws Exception
    {
        final DefaultConfiguration checkConfig = createConfiguration();

        final String[] expected = {
            "1: 'CommentedPackageVisibilityCheckDefaultPackageTestInput' "
                    + "should be commented for package visibility.",
            "3: 'NoCommentedPackageVisibilityClass' "
                    + "should be commented for package visibility.",
            "10: Is visibility of 'invalidCommentMethod' package?",
        };

        verify(checkConfig,
                getPath("CommentedPackageVisibilityCheckDefaultPackage"
                        + "TestInput.java"),
                expected);
    }
}
