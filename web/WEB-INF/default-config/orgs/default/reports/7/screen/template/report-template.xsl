<?xml version="1.0" encoding="us-ascii"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" encoding="US-ASCII"
		doctype-public="-//W3C//DTD XHTML 1.1//EN"
		doctype-system="http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd"
		indent="yes" />

	<xsl:template match="/">
		<html>
			<head>
				<link rel="stylesheet" type="text/css"
					href="{/source/request/@context-path}/orgs/1/reports/9/stream/output/" />

				<title>
					<xsl:value-of select="/source/org/@name" />
					&gt; Supplies &gt;
					<xsl:value-of select="/source/supply/@name" />
				</title>
			</head>

			<body>
				<p>
					<a
						href="{/source/request/@context-path}/orgs/{/source/org/@id}/reports/0/screen/output/">
						<xsl:value-of select="/source/org/@name" />
					</a>
					&gt;
					<a
						href="{/source/request/@context-path}/orgs/{/source/org/@id}/reports/49/screen/output/">
						<xsl:value-of select="'Supplies'" />
					</a>
					&gt;
					<a
						href="{/source/request/@context-path}/orgs/{/source/org/@id}/reports/3/screen/output/?supply-id={/source/supply/@id}">
						<xsl:value-of select="/source/supply/@name" />
					</a>
					&gt;
					<xsl:choose>
						<xsl:when
							test="/source/request/parameter[@name='is-import']/value = 'true'">
							Import
						</xsl:when>
						<xsl:otherwise>Export</xsl:otherwise>
					</xsl:choose>
					data by month
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
					<caption>Months</caption>
					<thead>
						<tr>
							<th>Month Starting</th>
							<th>MPAN Core</th>
							<th>MD / kW</th>
							<th>Power Factor</th>
							<th>MD / kVA</th>
							<th>Agreed Supply Capacity (kVA)</th>
							<th>kWh</th>
						</tr>
					</thead>
					<tbody>
						<xsl:for-each select="/source/month">
							<tr>
								<td>
									<xsl:value-of select="@date" />
								</td>
								<td>
									<xsl:choose>
										<xsl:when test="@mpan-core">
											<xsl:value-of
												select="@mpan-core" />
										</xsl:when>
										<xsl:otherwise>
											NA
										</xsl:otherwise>
									</xsl:choose>
								</td>
								<td>
									<xsl:choose>
										<xsl:when test="@md-kw">
											<xsl:value-of
												select="round(@md-kw)" />
										</xsl:when>
										<xsl:otherwise>
											NA
										</xsl:otherwise>
									</xsl:choose>
								</td>
								<td>
									<xsl:choose>
										<xsl:when test="@pf">
											<xsl:value-of select="@pf" />
										</xsl:when>
										<xsl:otherwise>
											NA
										</xsl:otherwise>
									</xsl:choose>
								</td>
								<td>
									<xsl:choose>
										<xsl:when test="@kva-at-md">
											<xsl:value-of
												select="round(@kva-at-md)" />
										</xsl:when>
										<xsl:otherwise>
											NA
										</xsl:otherwise>
									</xsl:choose>
								</td>
								<td>
									<xsl:choose>
										<xsl:when
											test="@agreed-supply-capacity">
											<xsl:value-of
												select="@agreed-supply-capacity" />
										</xsl:when>
										<xsl:otherwise>
											NA
										</xsl:otherwise>
									</xsl:choose>
								</td>
								<td>
									<xsl:choose>
										<xsl:when test="@total-kwh">
											<xsl:value-of
												select="round(@total-kwh)" />
										</xsl:when>
										<xsl:otherwise>
											NA
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

