# dropwizard-webjars-bundle

Resource for use in [Dropwizard](http://dropwizard.codahale.com) that makes it
a lot easier to work with [WebJars](http://www.webjars.org).

Regular java code doesn't need to know or care about what version of a
dependency it is using.  It simply imports the class by name and goes on about
its business.  Why shouldn't your front-end development work the same way?
This bundle automatically injects version information for you.


## Getting Started

Just add this maven dependency to get started:

```xml
<dependency>
    <groupId>com.bazaarvoice.dropwizard</groupId>
    <artifactId>dropwizard-webjars-bundle</artifactId>
    <version>0.1.0</version>
</dependency>
```

Add the resource to your environment:

```java
public class SampleService extends Service<Configuration> {
    public static void main(String[] args) throws Exception {
        new SampleService().run(args);
    }

    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
        bootstrap.addBundle(new WebJarBundle());
    }

    @Override
    public void run(Configuration cfg, Environment env) throws Exception {
        // ...
    }
}
```

Now reference your WebJar omitting version information:

```html
<script src="/webjars/bootstrap/js/bootstrap.min.js"></script>
```


## Customizing cache settings

By default the WebJar bundle has conservative, but reasonable, cache settings
to ensure that WebJar resources are returned quickly to your clients.  If for
some reason the built-in settings aren't suitable for your application they can
be overidden by invoking the `WebJarBundle` constructor with a
`com.google.common.cache.CacheBuilder` instance that is configured with
your desired caching settings.


## Customizing WebJar packages

Custom WebJars often appear in a package other than `org.webjars`.  In order to
support these custom WebJars, just invoke the `WebJarBundle` constructor with
a list of the package names you would like to be considered by the bundle.
