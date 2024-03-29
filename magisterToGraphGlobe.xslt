<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="/">
        <appointments>
            <xsl:for-each select="Response/Table/Personeelsleden/Personeelslid">
                <appointment>
                            <organizer>
                                <emailAddress>
                                    <address>
                                        <xsl:value-of select="Email_priv"/>
                                    </address>
                                </emailAddress>
                            </organizer>
                    <subject>
                        <xsl:value-of select="Subject"/>
                    </subject>
                    <body>
                        <contentType>HTML</contentType>
                        <content>
                            <xsl:value-of select="Body"/>
                        </content>
                    </body>
                    <start>
                        <dateTime>
                            <xsl:value-of select="Start"/>
                        </dateTime>
                        <timeZone>Europe/Berlin</timeZone>
                    </start>
                    <end>
                        <dateTime>
                            <xsl:value-of select="Finish"/>
                        </dateTime>
                        <timeZone>Europe/Berlin</timeZone>
                    </end>
                    <location>
                        <displayName>
                            <xsl:value-of select="Location"/>
                        </displayName>
                    </location>
                    <showAs>
                        <xsl:value-of select="OutlookStatus"/>
                    </showAs>
                    <transactionId>
                        <xsl:value-of select="concat(Code, '-', Id)"/>
                    </transactionId>
                    <isReminderOn>false</isReminderOn>
                </appointment>
            </xsl:for-each>
        </appointments>
    </xsl:template>
</xsl:stylesheet>



