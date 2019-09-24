import com.gu.contentapi.client.model.v1.{Tag, TagType}

val keywordTag = Tag("idKeyword", TagType.Keyword, None, None, "World", "", "")
val contributorTag = Tag("idContributor", TagType.Contributor, None, None, "Steve", "", "")
val contributorTag2 = Tag("idContributor2", TagType.Contributor, None, None, "Mike", "", "")
val seriesTag = Tag("idSeries", TagType.Series, None, None, "Rugby World Cup", "", "")
val seriesTag2 = Tag("idSeries2", TagType.Series, None, None, "Tetris World Cup", "", "")
val blogTag = Tag("idBlog", TagType.Blog, None, None, "blogTag", "", "")
val blogTag2 = Tag("idBlog2", TagType.Blog, None, None, "blogTag2", "", "")
