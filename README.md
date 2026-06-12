# Experiment-Aware PDP Automation

A reference automation framework for testing a Product Detail Page that's under several
simultaneous A/B experiments (CTA placement, price display, recommendations, a full
redesign). Java + Selenium + TestNG.

> A note on scope: the code is structured and realistic but targets an illustrative
> `shop.example.com` rather than a live site, so it demonstrates the design without shipping
> a mock server. The selectors, flow, and decisions are the same ones I'd use against a real
> target.

## The core idea

Experiments make UI tests flaky for two reasons: the page renders differently each run
(random bucketing), and tests are written against *where things are* instead of *what they
mean*. This framework attacks both.

**1. Make variants deterministic.** A test never hopes to land in a variant. It pins the
buckets it wants via a forced-variant cookie, then reads the decision API back to confirm
what actually rendered. If the pin failed, the test fails for an honest reason instead of
silently testing control. See `ExperimentResolver`.

**2. Make page objects experiment-aware, so tests don't have to be.** The test says
`pdp.addToCart()`. The page object figures out whether that's an inline button or a sticky
footer bar. Tests describe behaviour; page objects absorb layout. See `ProductDetailPage`
and `AddToCartCta`.

**3. Locate by meaning, not position.** Every interactive element is found by a stable
`data-testid` (a contract with the front-end team), with human-readable fallbacks as a
safety net. There is no positional XPath anywhere — that's a deliberate constraint, not an
oversight. See `SmartLocator`.

## How the pieces fit

```
test                 BaseTest.openPdp("SKU", {cta_placement: "sticky"})
  │                        │  pins variants, navigates, builds context
  ▼                        ▼
ProductDetailPage  ──►  ExperimentContext   (which variants are live this session)
  │   (facade)             ▲
  ├─ AddToCartCta          │ resolved from
  ├─ Price                 │
  └─ SmartLocator     ExperimentResolver ──► ExperimentApiClient ──► /api/experiments/decisions
```

On failure, `ExperimentReporter` (a TestNG listener) prints the exact set of active
assignments, so "which experiment broke this?" is answered in the log, not by a human
re-running locally.

## Page Object Model (hybrid)

This is a Page Object Model framework — tests call intent (`pdp.getPrice()`, `pdp.addToCart()`)
and never touch a locator or the driver. The page objects (`BasePage` → `ProductDetailPage`,
with `AddToCartCta` / `Price` as components) own all the locator detail.

It's deliberately a **hybrid** of the two POM flavours, because an experiment-heavy site
breaks the usual one-size rule:

- **Classic PageFactory (`@FindBy`)** for elements that are simple, stable, and present in
  the same place on every variant — e.g. the current price. `BasePage` runs
  `PageFactory.initElements` with an `AjaxElementLocatorFactory`, so those fields are wired
  automatically and come with a built-in wait. This is the canonical POM most reviewers expect.

- **`SmartLocator`** for everything the experiments actually move or drop — the title (a
  redesign relocates it), the add-to-cart CTA (top/bottom/sticky), and optional modules
  (compare-at price, recommendations). PageFactory binds *one* locator per field up front and
  caches the element reference, which is precisely wrong for elements that re-render or shift
  between variants, so those use ordered-fallback locators instead.

The rule encoded in the code: stable across variants → `@FindBy`; varies or might be absent →
`SmartLocator`. `ProductDetailPage.getPrice()` shows both in one method — `@FindBy` for the
mandatory current price, `SmartLocator` for the optional "was" price.

## Layout

```
src/main/java/com/shop/qa
  experiments/   ExperimentContext, ExperimentResolver, ExperimentApiClient, PdpExperiments
  core/          DriverFactory, SmartLocator, WaitUtils
  pages/         BasePage + pages/pdp: ProductDetailPage, AddToCartCta, Price
src/test/java/com/shop/qa
  BaseTest.java
  pdp/           PdpCoreFlowTest, PdpAddToCartTest, PdpVariantMatrixTest
  support/       ExperimentReporter (observability), TestContextHolder
testng.xml         suite: smoke (per-PR) + variant-matrix (nightly)
```

## Edge cases from the brief, and where they're handled

| Edge case | Where | How |
|---|---|---|
| CTA top / bottom / sticky | `AddToCartCta` | Same testid across positions; scroll-into-view + JS-click fallback when a sticky bar intercepts the click. No branching on position. |
| Missing element (experiment bug) | `SmartLocator.require` + `PdpVariantMatrixTest` | `require()` throws a readable, attributable error; the matrix test asserts the buy button exists for every variant. |
| Different UI structures, same function | `ProductDetailPage.titleLocator()` | Shared facade for skin-level variants; branch to a per-variant locator **only** for the redesign that actually restructures the DOM. |
| Optional module absent | `ProductDetailPage.hasRecommendations` | Soft check — logged, never fails the deploy. |

## Running it

```bash
# all suites, headless chrome, against a target
mvn test -DbaseUrl=https://shop.example.com

# just the per-PR smoke slice
mvn test -Dgroups=smoke

# nightly variant matrix
mvn test -Dgroups=variant-matrix

# against a grid / cloud (CI)
mvn test -DremoteUrl=$REMOTE_URL -Dbrowser=chrome
```

Local runs use Selenium Manager, so there are no driver binaries to install. CI points
`REMOTE_URL` at a grid and runs the same code headless.
