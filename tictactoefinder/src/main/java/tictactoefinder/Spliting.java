package tictactoefinder;

import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.real.DoubleType;

import java.util.ArrayList;
import java.util.Arrays;

public class Spliting extends Step {
    public static class Line implements Comparable{
        public int index;
        public double value;
        public Line(int index, double value)
        {
            this.index = index;
            this.value=value;
        }

        @Override
        public int compareTo(Object o) {
            if(this.value == ((Line)o).value)
                return 0;
            return this.value-((Line)o).value > 0 ? 1 : -1;
        }
    }
    public static long[][] detection(Img<DoubleType> img)
    {
        long[] imgSize = getDimensions(img);

        long[][] res = new long[9][];

        RandomAccess<DoubleType> imgCursor = img.randomAccess();
        Line[] horizontalDetect = new Line[(int)imgSize[0]];
        Line[] verticalDetect = new Line[(int)imgSize[1]];

        //Projection des lignes horizontales
        projection(imgSize, imgCursor, horizontalDetect, 0, 1);

        //Projection des lignes verticales
        projection(imgSize, imgCursor, verticalDetect, 1, 0);

        //On trie les lignes par valeur de la projection
        Line[] hDetect = horizontalDetect.clone();
        Line[] vDetect = verticalDetect.clone();
        Arrays.sort(hDetect);
        Arrays.sort(vDetect);

        ArrayList<Integer> detectedHLinesBegin = new ArrayList<>();
        ArrayList<Integer> detectedVLinesBegin = new ArrayList<>();
        ArrayList<Integer> detectedHLinesEnd = new ArrayList<>();
        ArrayList<Integer> detectedVLinesEnd = new ArrayList<>();

        //Détection des lignes horizontales principales
        detectLinesFromProjection(horizontalDetect, hDetect, detectedHLinesBegin, detectedHLinesEnd);
        if(detectedHLinesBegin.size() < 2) //On n'a trouvé trop peu de lignes, on va diviser en 3 parts egales
        {
            detectedHLinesBegin.add((int) (imgSize[0]/0.33333));
            detectedHLinesBegin.add((int) (imgSize[0]/0.66667));
            detectedHLinesEnd.add(detectedHLinesBegin.get(0)+1);
            detectedHLinesEnd.add(detectedHLinesBegin.get(1)+1);
        }else
        {
            decimateLines(horizontalDetect, detectedHLinesBegin, detectedHLinesEnd); //Si il y a trop de lignes détectées alors on ne garde que les plus contrastées
        }
        //Détection des lignes verticales principales
        detectLinesFromProjection(verticalDetect, vDetect, detectedVLinesBegin, detectedVLinesEnd);
        if(detectedVLinesBegin.size() < 2) //On n'a trouvé trop peu de lignes, on va diviser en 3 parts egales
        {
            detectedVLinesBegin.add((int) (imgSize[1]/0.33333));
            detectedVLinesBegin.add((int) (imgSize[1]/0.66667));
            detectedVLinesEnd.add(detectedVLinesBegin.get(0)+1);
            detectedVLinesEnd.add(detectedVLinesBegin.get(1)+1);
        }else {
            decimateLines(verticalDetect, detectedVLinesBegin, detectedVLinesEnd); //Si il y a trop de lignes détectées alors on ne garde que les plus contrastées
        }

        //On utilise les lignes trouvées comme bordures des cases du plateau
        res[0] =  new long[]{0, detectedHLinesBegin.get(0), 0, detectedVLinesBegin.get(0)};
        res[1] =  new long[]{detectedHLinesEnd.get(0), detectedHLinesBegin.get(1), 0, detectedVLinesBegin.get(0)};
        res[2] =  new long[]{detectedHLinesEnd.get(1), imgSize[0], 0, detectedVLinesBegin.get(0)};

        res[3] =  new long[]{0, detectedHLinesBegin.get(0), detectedVLinesEnd.get(0), detectedVLinesBegin.get(1)};
        res[4] =  new long[]{detectedHLinesEnd.get(0), detectedHLinesBegin.get(1), detectedVLinesEnd.get(0), detectedVLinesBegin.get(1)};
        res[5] =  new long[]{detectedHLinesEnd.get(1), imgSize[0], detectedVLinesEnd.get(0), detectedVLinesBegin.get(1)};

        res[6] =  new long[]{0, detectedHLinesBegin.get(0), detectedVLinesEnd.get(1), imgSize[1]};
        res[7] =  new long[]{detectedHLinesEnd.get(0), detectedHLinesBegin.get(1), detectedVLinesEnd.get(1), imgSize[1]};
        res[8] =  new long[]{detectedHLinesEnd.get(1), imgSize[0], detectedVLinesEnd.get(1), imgSize[1]};

        return res;
    }

    private static void decimateLines(Line[] detect, ArrayList<Integer> detectedLinesBegin, ArrayList<Integer> detectedLinesEnd) {
        if(detectedLinesBegin.size()>2) //On a trouvé trop de lignes, on va ne garder que les plus contrastés
        {
            Line[] values = new Line[detectedLinesBegin.size()];
            for(int i=0; i<values.length;i++) //Pour chaque ligne...
            {
                values[i]=new Line(i,0);
                for(int j = detectedLinesBegin.get(i); j< detectedLinesEnd.get(i); j++) //... on prend la valeur de la projection...
                {
                    values[i].value+= detect[j].value;
                }
                values[i].value/=(detectedLinesEnd.get(i)- detectedLinesBegin.get(i)); //... pour en faire une moyenne pour la ligne complete
            }

            Arrays.sort(values); //On trie les lignes moyennes par leur valeur

            ArrayList<Integer> tempBegin = new ArrayList<>();
            ArrayList<Integer> tempEnd = new ArrayList<>();

            //On ajoute la meilleure ligne à une liste temporaire...
            tempBegin.add(detectedLinesBegin.get(values[values.length-1].index));
            tempEnd.add(detectedLinesEnd.get(values[values.length-1].index));

            Integer begin = detectedLinesBegin.get(values[values.length-2].index);
            if(detectedLinesBegin.get(begin) > tempBegin.get(0)) //... dans le bon ordre
            {
                tempBegin.add(detectedLinesBegin.get(begin));
                tempEnd.add(detectedLinesEnd.get(begin));
            }else
            {
                tempBegin.add(0, detectedLinesBegin.get(begin));
                tempEnd.add(0, detectedLinesEnd.get(begin));
            }

            //Puis on modifie la liste principale afin de n'avoir que 2 lignes à l'intérieur
            detectedLinesBegin.clear();
            detectedLinesEnd.clear();
            detectedLinesBegin.addAll(tempBegin);
            detectedLinesEnd.addAll(tempEnd);
        }
    }

    private static void detectLinesFromProjection(Line[] detect, Line[] sortedDetect, ArrayList<Integer> detectedLinesBegin, ArrayList<Integer> detectedLinesEnd) {
        double percentileThreshold = 0.02;
        do {
            double threshold = sortedDetect[(int)(percentileThreshold* sortedDetect.length)].value;
            //On considère que les lignes sont des suites de projections ayant une valeur inférieure à un percentile des valeurs totales

            detectedLinesBegin.clear();
            detectedLinesEnd.clear();

            boolean inLine = false;
            for(int i=0; i<detect.length;i++)
            {
                if(detect[i].value <= threshold && !inLine) //Seuil atteint et on n'est pas dans une ligne => début d'une ligne
                {
                    detectedLinesBegin.add(i);
                    inLine = true;
                }
                if(detect[i].value > threshold && inLine) //Seuil non atteint et on était dans une ligne => fin d'une ligne
                {
                    detectedLinesEnd.add(i);
                    inLine = false;
                }
            }
            if(inLine)
            {
                detectedLinesEnd.add(detect.length-1);
            }
            if(detectedLinesBegin.size() != 2) //Si on n'a pas 2 lignes, alors on augmente le percentile et on recommence
            {
                percentileThreshold+=0.01;
            }else
            {
                break;
            }
        }while(percentileThreshold < 0.5);
    }

    private static void projection(long[] imgSize, RandomAccess<DoubleType> imgCursor, Line[] lines, int i2, int i3) {
        long[] pos = {0,0};
        for(int i = 0; i<imgSize[i2]; i++)
        {
            lines[i]=new Line(i,0);
            for(int j = 0; j<imgSize[i3]; j++)
            {
                pos[0]=i;
                pos[1]=j;
                imgCursor.setPosition(pos);
                lines[i].value+=imgCursor.get().getRealDouble();
            }
        }
    }

    public static ArrayList<Img<DoubleType>> split(Img<DoubleType> img, long[][] borders)
    {
        ArrayList<Img<DoubleType>> res = new ArrayList<>();
        RandomAccess<DoubleType> imgCursor = img.randomAccess();

        long[] size = new long[2];
        long[] pos = new long[2];
        long[] subPos = new long[2];

        for(int i=0; i<9;i++)
        {
            size[0] = borders[i][1] - borders[i][0];
            size[1] = borders[i][3] - borders[i][2];
            Img<DoubleType> sub = ArrayImgs.doubles(size);
            RandomAccess<DoubleType> subCursor = sub.randomAccess();
            //On crée une image qui sera la copie d'une case, telle que définie par les bordures des lignes trouvées par l'étape précédente

            for(long subx=0, x=borders[i][0]; subx < size[0]; subx++, x++)
            {
                subPos[0]=subx;
                pos[0]=x;
                for(long suby=0, y=borders[i][2]; subx < size[1]; suby++, y++)
                {
                    subPos[1]=suby;
                    pos[1]=y;
                    imgCursor.setPosition(pos);
                    subCursor.setPosition(subPos);

                    subCursor.get().set(imgCursor.get().getRealDouble());
                }
            }

            res.add(sub);
        }

        return res;
    }
}
