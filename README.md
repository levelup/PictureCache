PictureCache
============

Using a cache for image loading in Android. As used in Plume for Twitter.

The library uses UID for each cache entry. The same UID can have multiple URL sources over time.
When no UID is provided, one is generated based on the URL.
There are 3 different "buckets" for cache files: eternal, long term and short term.
A short term item can be transparently transfered to a longer term "bucket".
The database/files are purged automatically according to the storage size provided by the user for each "bucket".

The InMemoryDb library code can be found at https://github.com/robUx4/InMemoryDb