# dropwizard-webjars-bundle

A [Dropwizard](http://dropwizard.codahale.com) bundle that makes it
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
    <version>0.2.1</version>
</dependency>
```

- For Dropwizard 0.6.2: use version < 0.2.0
- For Dropwizard 0.7.0: use version >= 0.2.0

Add the resource to your environment:

```java
public class SampleApplication extends Application<Configuration> {
    public static void main(String[] args) throws Exception {
        new SampleApplication().run(args);
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

The cache that is built by the `WebJarBundle` will include a
`com.google.common.cache.Weigher` as part of it that indicates how many bytes
each resource is taking up in the cache.  If desired you can include a maximum
weight in your `CacheBuilder` to limit the amount of memory used by the cache.


## Customizing WebJar groups

Custom WebJars artifacts often appear in a maven group other than `org.webjars`.
In order to support these custom WebJars, just invoke the `WebJarBundle`
constructor with a list of the group names you would like to be considered by
the bundle.

Don't forget to also include `org.webjars` in your list if you want standard
WebJars to be found as well.
