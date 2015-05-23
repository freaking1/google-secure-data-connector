[Secure Data Connector](http://code.google.com/securedataconnector/) agent is a reference implementation of the Google Secure Data Connector protocol.  This protocol allows [Google Apps Premier](http://www.google.com/apps/intl/en/business/details.html) & [Google Apps for Education](http://www.google.com/a/help/intl/en/edu/index.html) customers to make available specific resources that Google's hosted applications (the cloud) can access inside the customer's network.  Differently stated, Secure Data Connector aims to solve the firewall-traversal problem bridging Google's applications with our customer's network to give our end users the experience of using a desktop application, but with all the benefits the cloud brings. Combined with Google Apps cloud based URLs, administrators have very fine grained control over how resources inside their network are connected to the cloud.

How it works for our [Google Apps Premier](http://www.google.com/apps/intl/en/business/details.html) & [Google Apps for Education](http://www.google.com/a/help/intl/en/edu/index.html) customers:

The customer network administrator downloads an open source application (the [Secure Data Connector](http://code.google.com/securedataconnector/)).  Using the configuration file, they configure which URL patterns and IP:port socket resources (here-by both referred to generically as "resources")  they wish to share with Google.   The administrator can pick which users from specific source applications are allowed to access each resource giving a fine grain of control over network access.   After configuration, the administrator directs the agent to connect a Google datacenter and authenticates using the predefined user account.  At this point, a customer can use any Secure-Data-Connector enabled Google application to access resources that reside within their network.


### Latest Version ###
1.3 Release Candidate 2: [1.3-rc2](http://code.google.com/p/google-secure-data-connector/downloads/detail?name=google-secure-data-connector-1.3-rc2-all.zip&can=2&q=)

**Bug fixes:**
  * Health check / keep-alive thread now starts on agent start-up.
  * `ResourceFileWatcher` now properly closes opened files.