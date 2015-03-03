# Cloudant SE Common Libraries

A set of common and reusable libraries for Cloudant

* [Notes](#notes)
* [Tests](#tests)
* [License](#license)
  
##Notes

Make sure that you use httpclient version > 4.3.6 or there is a significant chance you will run into errors on reading documents where the socket timeout will not be honoroed and your program will hang forever.

## Tests

To run the tests the following properties must be set.

The following can come from either system properties, environment or the default configuration file `src/test/resources/cloudant.properties`

* cloudant_test_account=account
* cloudant_test_database_prefix=database
* cloudant_test_user=user

The following can come from either system properties, environment but NOT the configuration file

* cloudant_test_password=password

`mvn test`

## License

Copyright 2014 Cloudant, an IBM company.

Licensed under the apache license, version 2.0 (the "license"); you may not use this file except in compliance with the license.  you may obtain a copy of the license at

    http://www.apache.org/licenses/LICENSE-2.0.html

Unless required by applicable law or agreed to in writing, software distributed under the license is distributed on an "as is" basis, without warranties or conditions of any kind, either express or implied. See the license for the specific language governing permissions and limitations under the license.

## Links

* [Issue Tracking](https://github.com/cavanaugh-ibm/se-common/issues)
* [Cloudant Query Docs](http://docs.cloudant.com/api/cloudant-query.html)
* [Cloudant Search Docs](http://docs.cloudant.com/api/search.html)
* [Cloudant Auth Docs](http://docs.cloudant.com/api/authz.html)
* [Cloudant Changes Follower](https://github.com/iriscouch/follow)
