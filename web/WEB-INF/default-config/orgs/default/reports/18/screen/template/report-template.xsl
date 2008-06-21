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
					href="{/source/request/@context-path}/orgs/{/source/channel-snags/dce-service/dce/org/@id}/reports/9/stream/output/" />

				<title>
					<xsl:value-of
						select="/source/channel-snags/dce-service/dce/org/@name" />
					&gt; DCEs &gt;
					<xsl:value-of
						select="/source/channel-snags/dce-service/dce/@name" />
					&gt; Services &gt;
					<xsl:value-of
						select="/source/channel-snags/dce-service/@name" />
				</title>

			</head>

			<body>
				<p>
					<a
						href="{/source/request/@context-path}/orgs/{/source/channel-snags/dce-service/dce/org/@id}/reports/0/screen/output/">
						<xsl:value-of
							select="/source/channel-snags/dce-service/dce/org/@name" />
					</a>
					&gt;
					<a
						href="{/source/request/@context-path}/orgs/{/source/channel-snags/dce-service/dce/org/@id}/reports/54/screen/output/">
						<xsl:value-of select="'DCEs'" />
					</a>
					&gt;
					<a
						href="{/source/request/@context-path}/orgs/{/source/channel-snags/dce-service/dce/org/@id}/reports/55/screen/output/?dce-id={/source/channel-snags/dce-service/dce/@id}">
						<xsl:value-of
							select="/source/channel-snags/dce-service/dce/@name" />
					</a>
					&gt;
					<a
						href="{/source/request/@context-path}/orgs/{/source/channel-snags/dce-service/dce/org/@id}/reports/56/screen/output/?dce-id={/source/channel-snags/dce-service/dce/@id}">
						<xsl:value-of select="'Services'" />
					</a>
					&gt;
					<a
						href="{/source/request/@context-path}/orgs/{/source/channel-snags/dce-service/dce/org/@id}/reports/57/screen/output/?service-id={/source/channel-snags/dce-service/@id}">
						<xsl:value-of
							select="/source/channel-snags/dce-service/@name" />
					</a>
					&gt;
					<xsl:value-of select="'Channel Snags ['" />
					<a
						href="{/source/request/@context-path}/orgs/{/source/channel-snags/dce-service/dce/org/@id}/dces/{/source/channel-snags/dce-service/dce/@id}/services/{/source/channel-snags/dce-service/@id}/channel-snags/">
						<xsl:value-of select="'edit'" />
					</a>
					<xsl:value-of select="']'" />
				</p>

				<p>
					<xsl:value-of
						select="source/channel-snags/@snag-count" />
					Snag(s) (older then 5 days) Total over
					<xsl:value-of
						select="source/channel-snags/@site-count" />
					Site(s)
				</p>
				<p>
					A further
					<xsl:value-of
						select="source/channel-snags/@pending-site-count" />
					site(s) have snags not yet older then 5 days
				</p>
				<table>
					<thead>
						<tr>
							<th>SnagId</th>
							<th>MPAN</th>
							<th>Site Name</th>
							<th>Snag Description</th>
							<th>Supply</th>
							<th>Units</th>
							<th>Duration</th>
						</tr>
					</thead>
					<tbody>
						<xsl:for-each
							select="/source/channel-snags/channel-snag">
							<tr>
								<td>
									<a
										href="{/source/request/@context-path}/orgs/{/source/channel-snags/dce-service/dce/org/@id}/reports/58/screen/output/?snag-id={@id}">
										<xsl:value-of select="@id" />
									</a>
									<xsl:value-of select="' ['" />
									<a
										href="{/source/request/@context-path}/orgs/{/source/channel-snags/dce-service/dce/org/@id}/dces/{/source/channel-snags/dce-service/dce/@id}/services/{/source/channel-snags/dce-service/@id}/channel-snags/{@id}/">
										<xsl:value-of select="'edit'" />
									</a>
									<xsl:value-of select="']'" />
								</td>

								<td>
									<xsl:apply-templates
										select="channel/supply/supply-generation/mpan" />
								</td>
								<td>
									<xsl:value-of
										select="channel/supply/site/@name" />
								</td>
								<td>
									<xsl:value-of select="@description" />
								</td>

								<td>
									<xsl:choose>
										<xsl:when
											test="channel/@is-import='true'">
											Import
										</xsl:when>
										<xsl:otherwise>
											Export
										</xsl:otherwise>
									</xsl:choose>
								</td>

								<td>
									<xsl:choose>
										<xsl:when
											test="channel/@is-kwh='true'">
											kWh
										</xsl:when>
										<xsl:otherwise>
											kVAr
										</xsl:otherwise>
									</xsl:choose>
								</td>

								<td>
									Start
									<xsl:apply-templates
										select="hh-end-date[@label='start']" />
									<br />
									Finish
									<xsl:apply-templates
										select="hh-end-date[@label='finish']" />
								</td>
							</tr>
						</xsl:for-each>
					</tbody>
				</table>

			</body>
		</html>
	</xsl:template>

	<xsl:template match="mpan">
		<xsl:value-of select="mpan-core/dso/@code" />
		<xsl:value-of select="mpan-core/@uniquePart" />
		<xsl:value-of select="mpan-core/@checkDigit" />
		<br />
	</xsl:template>

	<xsl:template match="hh-end-date">
		<xsl:value-of select="@year" />
		-
		<xsl:value-of select="@month" />
		-
		<xsl:value-of select="@day" />
		<xsl:text></xsl:text>
		<xsl:value-of select="@hour" />
		:
		<xsl:value-of select="@minute" />
		<xsl:text>Z</xsl:text>
	</xsl:template>
</xsl:stylesheet>