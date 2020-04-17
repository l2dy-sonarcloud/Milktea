package jp.panta.misskeyandroidclient.mfm

import java.util.regex.Pattern

object MFMParser{

    fun parse(text: String): Node{
        println("textSize:${text.length}")
        val root = Root(text)
        NodeParser(text, root).parse()
        return root
    }


    /**
     * @param start 担当する文字列のスタート地点
     * @param end 担当する文字列の終了地点
     * @param parent 親ノードこのParserはこの parentの内側の処理をしていることになる
     * つまりNodeParserとの関係はparent : NodeParserという一対一の関係になる。
     * <parent>child content</parent>
     *         ↑start       ↑end
     */
    class NodeParser(
        val sourceText: String,
        val parent: Node,
        val start: Int = parent.insideStart,
        val end: Int = parent.insideEnd
    ){
        // タグ探索開始
        // タグ探索中
        // タグ探索完了
        // タグ探索キャンセル

        private var position: Int = start

        /**
         * 一番最後にタグを検出したタグの最後のpositionがここに代入される。
         * recoveryBeforeText()されるときに使用される
         */
        private var finallyDetected: Int = start


        /**
         * 第一段階としてタグの先頭の文字に該当するかを検証する
         * キャンセルされたときはここからやり直される
         */
        val beginningOfStartTag = mapOf(
            '<' to ::parseBlock, //斜体、小文字、中央揃え、横伸縮、左右反転、回転、飛び跳ねる
            '~' to ::parseStrike, //打消し線
            '(' to ::parseExpansion, //横伸縮
            '`' to ::parseCode, //コード
            '>' to ::parseQuote, //引用
            '【' to ::parseTitle//タイトル
        )

        /**
         * 何にも該当しない文字を葉として追加する
         * @param tagStart 次に存在するNodeの始点
         * text<Node> この場合だと4になる
         */
        private fun recoveryBeforeText(tagStart: Int){
            // 文字数が０より多いとき
            if((tagStart - finallyDetected) > 0){
                val text = sourceText.substring(finallyDetected, tagStart)
                parent.childNodes.add(Text(text))
            }
        }

        fun parse(){
            while(position < end){
                val parser = beginningOfStartTag[sourceText[position]]

                if(parser == null){
                    // 何にも該当しない場合は繰り上げる
                    position ++
                }else{

                    // Nodeを取り出すときにpositionが変化することがあるので、その前にタグの始点を記録する
                    val tagStart = position
                    val node = parser.invoke()
                    // nodeが実際に存在したとき
                    if(node != null){

                        // positionは基本的にはNodeの開始地点のままなので発見したNodeの終了地点にする
                        position = node.end


                        // Nodeの直前のNodeに含まれないLeafの回収作業を行う
                        recoveryBeforeText(node.start)

                        // 新たに発見したnodeの一番最後の外側の文字を記録する
                        finallyDetected = node.end



                        // 発見したNodeを追加する
                        parent.childNodes.add(node)


                        // 新たに発見した子NodeのためにNodeParserを作成する
                        // 新たに発見した子Nodeの内側を捜索するのでparentは新たに発見した子Nodeになる
                        NodeParser(sourceText, parent = node).parse()


                    }else{
                        position ++
                    }
                }
            }
            //parent.endTag.start == position -> true
            //println(sourceText.substring(finallyDetected, parent.end))
            recoveryBeforeText(parent.insideEnd)

        }

        /**
         * タグの開始位置や終了位置、内部要素の開始、終了位置は正規表現とMatcherを利用し現在のポジションと合わせ相対的に求める
         */

        private fun parseBlock(): Node?{
            return null
        }

        private fun parseStrike(): Node?{
            return null
        }

        private fun parseExpansion(): Node?{
            return null
        }

        private fun parseCode(): Node?{
            return null
        }

        private fun parseQuote(): Node?{

            // 直前の文字がある場合
            if(position > 0){
                val c = sourceText[ position - 1 ]
                // 直前の文字が改行コードではないかつ、親が引用コードではない
                if( (c != '\r' && c != '\n') && parent.tag != TagType.QUOTE){
                    return null
                }
            }
            val quotePattern = Pattern.compile("""^>(?:[ ]?)([^\n\r]+)(\n\r|\n)?""", Pattern.MULTILINE)
            val matcher = quotePattern.matcher(sourceText.substring(position, parent.insideEnd))

            val inside = StringBuilder()
            var nodeEnd = position

            while(true){
                if(!matcher.find()) break
                nodeEnd = matcher.end()
                if(inside.isNotEmpty()){
                    inside.append('\n')
                }
                inside.append(matcher.group(1))
            }


            // > の後に何もない場合キャンセルする
            if(nodeEnd + position <= position){
                return null
            }


            val node =  Node(
                start = position,
                end = nodeEnd + position,
                tag = TagType.QUOTE,
                insideStart = position + 1, // >を排除する
                insideEnd = position + nodeEnd,
                parentNode = parent
            )
            return node
        }

        private fun parseTitle(): Node?{
            return null
        }


    }
}