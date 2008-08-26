<?xml version="1.0" encoding="us-ascii"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html" encoding="US-ASCII"
		doctype-public="-//W3C//DTD HTML 4.01//EN"
		doctype-system="http://www.w3.org/TR/html4/strict.dtd" indent="yes" />

	<xsl:template match="/">
		<html>
			<head>
				<link rel="stylesheet" type="text/css"
					href="{/source/request/@context-path}/style/" />
				<title>
					Chellow &gt; DSOs &gt;
					<xsl:value-of
						select="concat(/source/mpan-tops/dso/@code, ' &gt; MPAN tops')" />
				</title>
			</head>

			<body>
				<p>
					<a href="{/source/request/@context-path}/">
						<img
							src="{/source/request/@context-path}/logo/" />
						<span class="logo">Chellow</span>
					</a>
					<xsl:value-of select="' &gt; '" />
					<a
						href="{/source/request/@context-path}/dsos/">
						<xsl:value-of select="'DSOs'" />
					</a>
					<xsl:value-of select="' &gt; '" />
					<a
						href="{/source/request/@context-path}/dsos/{/source/mpan-tops/dso/@id}/">
						<xsl:value-of
							select="/source/mpan-tops/dso/@code" />
					</a>
					<xsl:value-of select="' &gt; Mpan tops '" />
				</p>

				<xsl:if test="//message">
					<ul>
						<xsl:for-each select="//message">
							<li>
								<xsl:value-of select="@description" />
							</li>
						</xsl:for-each>
					</ul>
				</xsl:if>
				<table>
					<thead>
						<tr>
							<th>Chellow Id</th>
							<th>PC</th>
							<th>MTC</th>
							<th>LLFC</th>
							<th>SSC</th>
							<th>Valid From</th>
							<th>Valid To</th>
						</tr>
					</thead>
					<tbody>
						<xsl:for-each
							select="/source/mpan-tops/mpan-top">
							<tr>
								<td>
									<a href="{@id}/">
										<xsl:value-of select="@id" />
									</a>
								</td>
								<td>
									<xsl:value-of
										select="concat(pc/@code, ' - ', pc/@description)" />
								</td>
								<td>
									<xsl:value-of
										select="concat(mtc/@code, ' - ', mtc/@description)" />
								</td>
								<td>
									<xsl:value-of
										select="concat(llfc/@code, ' - ', llfc/@description)" />
								</td>
								<td>
									<xsl:value-of
										select="concat(ssc/@code, ' - ', ssc/@description)" />
								</td>
								<td>
									<xsl:value-of
										select="concat(date[@label='from']/@year, '-', date[@label='from']/@month, '-', date[@label='from']/@day, ' ', date[@label='from']/@hour, ':', date[@label='from']/@minute, ' Z')" />
								</td>
								<td>
									<xsl:choose>
										<xsl:when
											test="date[@label='to']">
											<xsl:value-of
												select="concat(date[@label='to']/@year, '-', date[@label='to']/@month, '-', date[@label='to']/@day, ' ', date[@label='to']/@hour, ':', date[@label='to']/@minute, ' Z')" />
										</xsl:when>
										<xsl:otherwise>
											Ongoing
										</xsl:otherwise>
									</xsl:choose>
								</td>
							</tr>
						</xsl:for-each>
					</tbody>
				</table>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>