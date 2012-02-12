Commented package visibility check (Checkstyle plugin)

A plugin of Checkstyle (http://checkstyle.sourceforge.net/) that checks
whether package visibility is commented instead of modifier. For example,
"/* package */".

Required softwares :

  * Checkstyle 5.5 or above (http://checkstyle.sourceforge.net/)

Usage :

  1. Add module "CommentedPackageVisibilityCheck" as a submodule of TreeWalker to
     your Checkstyle configuration.

     For example :

      <module name="TreeWalker">
        <module name="CommentedPackageVisibilityCheck" />
      </module>

  2. Add JAR of this plugin to classpath to run Checkstyle.


Properties of CommentedPackageVisibilityCheck :

  format : Pattern of package visibility comment.
           Default is "/\* package \*/" ("/* package */").

  requireLatterWhiteSpace : Controls whether to require white space after
                            package visibility comment.
                            Default is true.


NOTICE for developers :

  When you run tests without Maven 3, you must need to set
  the following VM arguments :

  -Dtestinputs.dir=src/testinputs/com/github/mikanbako/checkstyle/commentedpackagevisibilitycheck
