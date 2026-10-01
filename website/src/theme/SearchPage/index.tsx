import React, {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import Head from '@docusaurus/Head';
import Link from '@docusaurus/Link';
import {translate} from '@docusaurus/Translate';
import {usePluralForm} from '@docusaurus/theme-common';
import clsx from 'clsx';
import {Search, X} from 'lucide-react';
// Plugin internals — theme SearchPage override
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-expect-error — no types for package deep paths
import useSearchQuery from '@easyops-cn/docusaurus-search-local/dist/client/client/theme/hooks/useSearchQuery';
// @ts-expect-error — no types for package deep paths
import {
  fetchIndexesByWorker,
  searchByWorker,
} from '@easyops-cn/docusaurus-search-local/dist/client/client/theme/searchByWorker';
// @ts-expect-error — no types for package deep paths
import {SearchDocumentType} from '@easyops-cn/docusaurus-search-local/dist/client/shared/interfaces';
// @ts-expect-error — no types for package deep paths
import {highlight} from '@easyops-cn/docusaurus-search-local/dist/client/client/utils/highlight';
// @ts-expect-error — no types for package deep paths
import {highlightStemmed} from '@easyops-cn/docusaurus-search-local/dist/client/client/utils/highlightStemmed';
// @ts-expect-error — no types for package deep paths
import {getStemmedPositions} from '@easyops-cn/docusaurus-search-local/dist/client/client/utils/getStemmedPositions';
// @ts-expect-error — no types for package deep paths
import {concatDocumentPath} from '@easyops-cn/docusaurus-search-local/dist/client/client/utils/concatDocumentPath';
// @ts-expect-error — no types for package deep paths
import {
  Mark,
  searchContextByPaths,
  useAllContextsWithNoSearchContext,
} from '@easyops-cn/docusaurus-search-local/dist/client/client/utils/proxiedGenerated';
// @ts-expect-error — no types for package deep paths
import {normalizeContextByPath} from '@easyops-cn/docusaurus-search-local/dist/client/client/utils/normalizeContextByPath';
// @ts-expect-error — no types for package deep paths
import LoadingRing from '@easyops-cn/docusaurus-search-local/dist/client/client/theme/LoadingRing/LoadingRing';

export default function SearchPage(): ReactNode {
  return (
    <Layout>
      <SearchPageContent />
    </Layout>
  );
}

function SearchPageContent(): ReactNode {
  const {
    siteConfig: {baseUrl},
    i18n: {currentLocale},
  } = useDocusaurusContext();
  const {selectMessage} = usePluralForm();
  const {
    searchValue,
    searchContext,
    searchVersion,
    updateSearchPath,
    updateSearchContext,
  } = useSearchQuery();
  const [searchQuery, setSearchQuery] = useState(searchValue);
  const [searchResults, setSearchResults] = useState<
    Array<{
      document: {
        i: string | number;
        u: string;
        h?: string;
        t: string;
        s?: string;
        b: string[];
      };
      type: number;
      page: {t: string; b: string[]};
      tokens: string[];
      metadata: unknown;
    }>
    | undefined
  >();
  const versionUrl = `${baseUrl}${searchVersion}`;

  const pageTitle = useMemo(
    () =>
      searchQuery
        ? translate(
            {
              id: 'theme.SearchPage.existingResultsTitle',
              message: 'Search results for "{query}"',
              description: 'The search page title for non-empty query',
            },
            {query: searchQuery},
          )
        : translate({
            id: 'theme.SearchPage.emptyResultsTitle',
            message: 'Search the documentation',
            description: 'The search page title for empty query',
          }),
    [searchQuery],
  );

  useEffect(() => {
    updateSearchPath(searchQuery);
    if (searchQuery) {
      void (async () => {
        const results = await searchByWorker(
          versionUrl,
          searchContext,
          searchQuery,
          100,
        );
        setSearchResults(results);
      })();
    } else {
      setSearchResults(undefined);
    }
    // updateSearchPath intentionally omitted — would overflow the stack
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchQuery, versionUrl, searchContext]);

  const handleSearchInputChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      setSearchQuery(e.target.value);
    },
    [],
  );

  const clearSearch = useCallback(() => {
    setSearchQuery('');
  }, []);

  useEffect(() => {
    if (searchValue && searchValue !== searchQuery) {
      setSearchQuery(searchValue);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchValue]);

  const [searchWorkerReady, setSearchWorkerReady] = useState(false);
  useEffect(() => {
    async function doFetchIndexes() {
      if (
        !Array.isArray(searchContextByPaths) ||
        searchContext ||
        useAllContextsWithNoSearchContext
      ) {
        await fetchIndexesByWorker(versionUrl, searchContext);
      }
      setSearchWorkerReady(true);
    }
    void doFetchIndexes();
  }, [searchContext, versionUrl]);

  const hasContext = Array.isArray(searchContextByPaths);

  return (
    <>
      <Head>
        <meta property="robots" content="noindex, follow" />
        <title>{pageTitle}</title>
      </Head>

      <div className="sc-search-page">
        <h1 className="sc-search-page__title">{pageTitle}</h1>

        <div
          className={clsx(
            'sc-search-page__controls',
            hasContext && 'sc-search-page__controls--split',
          )}
        >
          <div className="sc-search-page__field">
            <Search
              className="sc-search-page__icon"
              size={18}
              strokeWidth={2.25}
              aria-hidden
            />
            <input
              type="search"
              name="q"
              className="sc-search-page__input"
              aria-label="Search"
              placeholder="Search documentation"
              onChange={handleSearchInputChange}
              value={searchQuery}
              autoComplete="off"
              autoFocus
            />
            {searchQuery ? (
              <button
                type="button"
                className="sc-search-page__clear"
                aria-label="Clear search"
                title="Clear"
                onClick={clearSearch}
              >
                <X size={18} strokeWidth={2.25} aria-hidden />
              </button>
            ) : null}
          </div>

          {hasContext ? (
            <select
              name="search-context"
              className="sc-search-page__context"
              id="context-selector"
              value={searchContext}
              onChange={(e) => updateSearchContext(e.target.value)}
            >
              {useAllContextsWithNoSearchContext && (
                <option value="">
                  {translate({
                    id: 'theme.SearchPage.searchContext.everywhere',
                    message: 'Everywhere',
                  })}
                </option>
              )}
              {searchContextByPaths.map(
                (context: string | {label: string; path: string}) => {
                  const {label, path} = normalizeContextByPath(
                    context,
                    currentLocale,
                  );
                  return (
                    <option key={path} value={path}>
                      {label}
                    </option>
                  );
                },
              )}
            </select>
          ) : null}
        </div>

        {!searchWorkerReady && searchQuery ? (
          <div className="sc-search-page__loading">
            <LoadingRing />
          </div>
        ) : null}

        {searchResults ? (
          searchResults.length > 0 ? (
            <p className="sc-search-page__count">
              {selectMessage(
                searchResults.length,
                translate(
                  {
                    id: 'theme.SearchPage.documentsFound.plurals',
                    message: '1 document found|{count} documents found',
                    description:
                      'Pluralized label for "{count} documents found". Use as much plural forms (separated by "|") as your language support (see https://www.unicode.org/cldr/cldr-aux/charts/34/supplemental/language_plural_rules.html)',
                  },
                  {count: searchResults.length},
                ),
              )}
            </p>
          ) : process.env.NODE_ENV === 'production' ? (
            <p className="sc-search-page__count">
              {translate({
                id: 'theme.SearchPage.noResultsText',
                message: 'No documents were found',
                description: 'The paragraph for empty search result',
              })}
            </p>
          ) : (
            <p className="sc-search-page__count">
              The search index is only available when you run docusaurus build!
            </p>
          )
        ) : null}

        <section className="sc-search-page__results">
          {searchResults?.map((item) => (
            <SearchResultItem key={item.document.i} searchResult={item} />
          ))}
        </section>
      </div>
    </>
  );
}

function SearchResultItem({
  searchResult: {document, type, page, tokens, metadata},
}: {
  searchResult: {
    document: {
      i: string | number;
      u: string;
      h?: string;
      t: string;
      s?: string;
      b: string[];
    };
    type: number;
    page: {t: string; b: string[]};
    tokens: string[];
    metadata: unknown;
  };
}): ReactNode {
  const isTitle = type === SearchDocumentType.Title;
  const isKeywords = type === SearchDocumentType.Keywords;
  const isDescription = type === SearchDocumentType.Description;
  const isDescriptionOrKeywords = isDescription || isKeywords;
  const isTitleRelated = isTitle || isDescriptionOrKeywords;
  const isContent = type === SearchDocumentType.Content;
  const pathItems = (isTitle ? document.b : page.b).slice();
  const articleTitle =
    isContent || isDescriptionOrKeywords ? document.s : document.t;
  if (!isTitleRelated) {
    pathItems.push(page.t);
  }
  let search = '';
  if (Mark && tokens.length > 0) {
    const params = new URLSearchParams();
    for (const token of tokens) {
      params.append('_highlight', token);
    }
    search = `?${params.toString()}`;
  }

  return (
    <article className="sc-search-page__item">
      <h2 className="sc-search-page__item-title">
        <Link
          to={document.u + search + (document.h || '')}
          dangerouslySetInnerHTML={{
            __html:
              isContent || isDescriptionOrKeywords
                ? highlight(articleTitle, tokens)
                : highlightStemmed(
                    articleTitle,
                    getStemmedPositions(metadata, 't'),
                    tokens,
                    100,
                  ),
          }}
        />
      </h2>
      {pathItems.length > 0 ? (
        <p className="sc-search-page__item-path">
          {concatDocumentPath(pathItems)}
        </p>
      ) : null}
      {(isContent || isDescription) && (
        <p
          className="sc-search-page__item-summary"
          dangerouslySetInnerHTML={{
            __html: highlightStemmed(
              document.t,
              getStemmedPositions(metadata, 't'),
              tokens,
              100,
            ),
          }}
        />
      )}
    </article>
  );
}
