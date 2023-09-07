# App Entry Listener

APIs that provide abstract superclasses to be used whenever a piece of code needs to use high-level events on pages that are application entries of a specific application: added, deleted, renamed, copied, etc.
Writing this using a raw event listener is a little complex because some events are higher level events while others are lower level events and such a listener would always need to do the same analysis and filtering of the event before handling it. This API would provide abstract superclasses to be used to manage such events easier, without worrying about the filtering or detection.

* Project Lead: [lucaa](https://www.xwiki.org/xwiki/bin/view/XWiki/lucaa)
* Documentation & Downloads: [Documentation & Download](https://extensions.xwiki.org/xwiki/bin/view/Extension/(extension name)))
* [Issue Tracker](https://jira.xwiki.org/browse/(jira id)
* Communication: [Forum](https://forum.xwiki.org/), [Chat](https://dev.xwiki.org/xwiki/bin/view/Community/Chat)
* [Development Practices](https://dev.xwiki.org)
* Minimal XWiki version supported: XWiki 14.4 - feel free to contribute a lowering of this version, as soon as proper functioning is validated.
* License: LGPL 2.1
* Translations: N/A
* Continuous Integration Status: [![Build Status](https://ci.xwiki.org/job/XWiki%20Contrib/job/(project id on ci)/job/master/badge/icon)](https://ci.xwiki.org/job/XWiki%20Contrib/job/(projct id on ci)/job/master/)
