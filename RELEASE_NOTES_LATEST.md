Documentation: https://typedb.com/docs/core-concepts/drivers/overview

## Distribution

#### Rust driver

Available from https://crates.io/crates/typedb-driver
Documentation: https://typedb.com/docs/drivers/rust/overview

```sh
cargo add typedb-driver@3.14.0
```


### Java driver

Available through [https://repo.typedb.com](https://cloudsmith.io/~typedb/repos/public-release/packages/detail/maven/typedb-driver/3.14.0/a=noarch;xg=com.typedb/)
Documentation: https://typedb.com/docs/drivers/java/overview

```xml
<repositories>
    <repository>
        <id>repo.typedb.com</id>
        <url>https://repo.typedb.com/public/public-release/maven/</url>
    </repository>
</repositories>
<dependencies>
    <dependency>
        <groupid>com.typedb</groupid>
        <artifactid>typedb-driver</artifactid>
        <version>3.14.0</version>
    </dependency>
</dependencies>
```

### Python driver

PyPI package: https://pypi.org/project/typedb-driver
Documentation: https://typedb.com/docs/drivers/python/overview

Available through https://pypi.org

[//]: # (TODO: Python's RC/Alpha/Beta versions are formatted differently. Don't foget to update manually until we make an automation)
```
pip install typedb-driver==3.14.0
```

### C# driver

NuGet package: https://www.nuget.org/packages/TypeDB.Driver
Documentation: https://typedb.com/docs/drivers/csharp/overview

```xml
<ItemGroup>
    <PackageReference Include="TypeDB.Driver" Version="3.14.0" />
    <PackageReference Include="TypeDB.Driver.Pinvoke.osx-x64" Version="3.14.0" />
    <PackageReference Include="TypeDB.Driver.Pinvoke.linux-x64" Version="3.14.0" />
    <PackageReference Include="TypeDB.Driver.Pinvoke.win-x64" Version="3.14.0" />
    <PackageReference Include="TypeDB.Driver.Pinvoke.osx-arm64" Version="3.14.0" />
    <PackageReference Include="TypeDB.Driver.Pinvoke.linux-arm64" Version="3.14.0" />
</ItemGroup>
```

### HTTP Typescript driver

NPM package: https://www.npmjs.com/package/@typedb/driver-http
Documentation: https://typedb.com/docs/home/install/drivers/

```
npm install @typedb/driver-http@3.14.0
```

### C driver

Compiled distributions comprising headers and shared libraries available at: https://cloudsmith.io/~typedb/repos/public-release/packages/?q=name:^typedb-driver-clib+version:3.14.0


## New Features


## Bugs Fixed
- **Put correct integers in concept documents**
  
  Fix incorrect number values returned in `fetch` documents.
  
  Integer values in concept documents were returned as doubles, so any value past 2^53 came back rounded. E.g., `i64::MAX` read as `9223372036854775808`. Fetching an integer attribute now returns it exactly in every driver.
  
  Three more faults in the same area are fixed additionally:
  - a whole double rendered as `2`, indistinguishable from an integer
  - Java and C# wrapped strings in quotes without escaping, so a value containing a quote, a backslash, or a control character produced output that is not valid JSON. Object keys were written that way in every driver that renders a document, so a fetch key holding a quote broke the document for the drivers that re-parse it
  - HTTP-TS rounded integers and could not send one back
  
  **Breaking changes:**
  1. `JSON` gains an `Integer(i64)` variant and renames `Number(f64)` to `Double(f64)`, so every existing match on `JSON::Number` must be updated.
  2. `JSON::Double(2.0).to_string()` is now `"2.0"` rather than `"2"`. Doubles are rendered with `Debug`, so some values change shape rather than only gaining a suffix: `1e-7` and `1.5e300` appear where a full decimal expansion did.
  3. HTTP-TS returns `bigint` for integers from 2^53 up, which a `number` cannot hold exactly.
  4. Java `JSON` and C# `IJSON` split `Number` into `Integer` and `Double` for explicit precision: `isNumber`/`asNumber` (`IsNumber`/`AsNumber`) are replaced by `isInteger`/`asInteger` and `isDouble`/`asDouble` (`IsInteger`/`AsInteger`, `IsDouble`/`AsDouble`).
  ### Example
  
  **Data** (`i` is `integer`, `d` is `double`, `dec` is `decimal`):
  ```
  math::write>> insert $p isa person, has i 9007199254740992, has d 9007199254740992.56738, has dec 9007199254740992.56738dec;
  *math::write>> insert $p isa person, has i 10007199254740992, has d 10007199254740992.56738111, has dec 10007199254740992.56738111dec;
  *math::write>> insert $p isa person, has i 7199254740992, has d 7199254740992.56738111, has dec 7199254740992.56738111dec;
  *math::write>> insert $p isa person, has i 9007199254740993, has d 9007199254740993.56738, has dec 9007199254740993.56738dec;
  ```
  Notice that the last digits are the same for each entity: `2`, `2`, `2`, `3`. 
  
  **Before:**
  ```
  math::read>> match $p isa person; fetch { $p.* };
  
  Finished read query validation and compilation...
  Streaming documents...
  {
    "d": 9007199254740992, // correct
    "dec": "9007199254740992.56738dec",
    "i": 9007199254740992 // correct
  }
  {
    "d": 10007199254740992,
    "dec": "10007199254740992.56738111dec",
    "i": 10007199254740992
  }
  {
    "d": 7199254740992.567,
    "dec": "7199254740992.56738111dec",
    "i": 7199254740992
  }
  {
    "d": 9007199254740994, // double cannot fit, natural
    "dec": "9007199254740993.56738dec",
    "i": 9007199254740992 // INCORRECT - must be 3 at the end!
  }
  Finished. Total answers: 4
  ```
  
  AFTER:
  ```
  math::read>> match $p isa person; fetch { $p.* };
  
  Finished read query validation and compilation...
  Streaming documents...
  {
    "d": 9007199254740992.0,
    "dec": "9007199254740992.56738dec",
    "i": 9007199254740992 // correct - 2 at the end
  }
  {
    "d": 1.0007199254740992e+16,
    "dec": "10007199254740992.56738111dec",
    "i": 10007199254740992
  }
  {
    "d": 7199254740992.567,
    "dec": "7199254740992.56738111dec",
    "i": 7199254740992
  }
  {
    "d": 9007199254740994.0, // double cannot fit, natural
    "dec": "9007199254740993.56738dec",
    "i": 9007199254740993 // correct - 3 at the end
  }
  Finished. Total answers: 4
  ```
  
  
- **Fix native formatting of negative fractional decimals**
  
  Fixes #929.
  
  Negative non-integer decimals use a floor-integer component and a nonnegative fractional component. The native formatter currently concatenates those components, so the value `-9.59` appears as `-10.41dec` in debug output and concept-document results even when its numeric getter is correct.
  
  This change prints the correct sign and magnitude. It does not modify stored values, protocol conversion, Python Decimal-context behavior, or whole-number formatting.
  
  

## Code Refactors


## Other Improvements
- **Update dependencies to include ordered toml files**
  Update dependencies to include ordered toml files
  
  
- **Prepare release 3.14.0-rc0**
  Prepare release 3.14.0-rc0
  
  
  
- **CI uses prebuilt x86_64 JDK for mac x86_64 tests**
  
  The Intel-prefix Homebrew has no x86_64 openjdk bottle on the current mac CI image, so 'brew install maven' compiled openjdk from source and hit the 10m no-output timeout when testing mac snapshots on x86.
  
  We now first install the whole JDK binary (Temurin 21 x86_64) as a cask, which is a prebuilt .pkg, then install maven via the native arm64 brew. We then set JAVA_HOME to the x86_64 JDK for tests.
  
  
- **Fix cargo build**

    
