<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="/">
        <appointments>
            <xsl:for-each select="appointments/*[name()='ro:TRowData']">
                <appointment>
                    <organizer>
                        <emailAddress>
                            <address>
                                <xsl:value-of select="concat(string[1], '@speykintegrations.onmicrosoft.com')"/>
                            </address>
                        </emailAddress>
                    </organizer>
                    <subject>
                        <xsl:value-of select="string[3]"/>
                    </subject>
                    <body>
                        <contentType>HTML</contentType>
                        <content>
                            <xsl:value-of select="string[5]"/>
                        </content>
                    </body>
                    <start>
                        <dateTime>
                            <xsl:value-of select="string[6]"/>
                        </dateTime>
                        <timeZone>Europe/Berlin</timeZone>
                    </start>
                    <end>
                        <dateTime>
                            <xsl:value-of select="string[7]"/>
                        </dateTime>
                        <timeZone>Europe/Berlin</timeZone>
                    </end>
                    <location>
                        <displayName>
                            <xsl:value-of select="string[4]"/>
                        </displayName>
                    </location>
                    <showAs>
                        <xsl:value-of select="string[10]"/>
                    </showAs>
                    <transactionId>
                        <xsl:value-of select="concat(string[1], '-', string[2])"/>
                    </transactionId>
                </appointment>
            </xsl:for-each>
        </appointments>
    </xsl:template>
</xsl:stylesheet>



