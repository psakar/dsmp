Welcome to the Dead Simple Maven Proxy (DSMP).

When using maven, you're sometimes faced with these issues:

- You want maven to use a certain mirror site
- All the developers in your team should use the same plugins.
- An official release is broken or needs a patch to work for you
- How do I allow maven to use a SNAPSHOT of some plugin without getting
all the other snapshots?
- You compile some project which insists to download dependencies from some
obscure site.
- You want all the developers in your team to use the same plugins.
- There is an insane firewall installed in your company which just denies
to download the odd JAR from the official maven repository.
- You want to use the mirror settings of maven (in settings.xml) but not all
your projects use the same mirror ids. For example, one project things
"codehaus.org" is for the release site, while another expects to see the
SNAPSHOTs there.

See the website for the full documentation:
http://www.pdark.de/dsmp/
